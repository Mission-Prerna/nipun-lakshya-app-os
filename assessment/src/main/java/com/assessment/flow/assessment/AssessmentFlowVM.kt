package com.assessment.flow.assessment

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.assessment.R
import com.assessment.flow.AssessmentConstants
import com.assessment.flow.scoreboard.StudentScoreboardActivity
import com.assessment.flow.workflowengine.bolo.ReadAlongProperties
import com.data.FlowType
import com.data.db.models.entity.AssessmentState
import com.data.db.models.helper.AssessmentStateDetails
import com.data.db.models.helper.FlowStateStatus
import com.data.models.stateresult.AssessmentStateResult
import com.data.repository.AssessmentsRepository
import com.data.repository.MetadataRepository
import com.google.gson.Gson
import com.samagra.commons.basemvvm.BaseViewModel
import com.samagra.commons.models.schoolsresponsedata.SchoolsData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AssessmentFlowVM
@Inject constructor(
    application: Application,
    private val assessmentsRepository: AssessmentsRepository,
    private val metadataRepository: MetadataRepository
) : BaseViewModel(application = application) {
    var schoolData: SchoolsData? = null
    var studentId: String = ""
    var grade: Int = -1
    private val stateFlow: MutableStateFlow<AssessmentFlowState> =
        MutableStateFlow(AssessmentFlowState.Loading(true))
    val eventsState = stateFlow.asStateFlow()
    val backgroundWorkCompleted = MutableLiveData<Boolean>()
    private lateinit var cachedAssessmentState: AssessmentStateDetails

    private var flowStarted = false
    private var flowInterrupted = false

    var readAlongProps: ReadAlongProperties? = null

    fun initFlow() {
        // Check if got the valid parameters
        if (grade == -1 || studentId.isBlank()) {
            stateFlow.value =
                AssessmentFlowState.OnExit(
                    (getApplication() as Context).getString(
                        R.string.invalid_assessment
                    )
                )
            return
        }
        refreshFlow()
        createStates()
    }

    private fun createStates() {
        val competencyIdsList: MutableList<Int> = mutableListOf()
        viewModelScope.launch(Dispatchers.IO) {
            val competencyList = metadataRepository.getCompetencies(grade)
            competencyIdsList.addAll(competencyList.map { it.id })
            val refList = metadataRepository.getRefIdsFromCompetencyIds(competencyIdsList)
            val assessmentStates: MutableList<AssessmentState> = mutableListOf()
            for (i in refList) {
                assessmentStates.add(
                    AssessmentState(
                        studentId = studentId,
                        competencyId = i.competencyId,
                        refIds = i.refIds,
                        flowType = if (i.type.equals("odk", true)) FlowType.ODK else FlowType.BOLO,
                        stateStatus = FlowStateStatus.PENDING
                    )
                )
            }
            assessmentsRepository.clearStates()
            Timber.i(
                "Assessments Registered : %s",
                assessmentsRepository.insertAssessmentStates(assessmentStates)
            )
        }

    }

    private fun observeStates() {
        // Start observing assessment states
        viewModelScope.launch {
            assessmentsRepository.observerIncompleteStates().collect {
                Timber.i("Assessments callbacks [${it.size}] : %s", Gson().toJson(it))
                if (it.isNotEmpty()) {
                    flowStarted = true
                    cachedAssessmentState = it[0]
                    stateFlow.value = AssessmentFlowState.Next(cachedAssessmentState)
                } else if (flowInterrupted) {
                    stateFlow.value = AssessmentFlowState.OnExit(
                        (getApplication() as Context).getString(
                            R.string.assessment_cancelled
                        )
                    )
                } else if (flowStarted) {
                    stateFlow.value = AssessmentFlowState.Completed
                }
            }
        }
    }

    private fun refreshFlow(isCancelledByUser: Boolean = false) {
        flowInterrupted = isCancelledByUser
        viewModelScope.launch {
            assessmentsRepository.clearStatesAsync()
            observeStates()
        }
    }

    fun markAssessmentComplete(
        state: AssessmentState,
        assessmentStateResult: AssessmentStateResult
    ) {
        state.stateStatus = FlowStateStatus.COMPLETED
        state.result = Gson().toJson(assessmentStateResult)
        viewModelScope.launch {
            assessmentsRepository.updateState(state)
        }
    }

    fun abandonFlow(
        state: AssessmentStateDetails,
        assessmentStateResult: AssessmentStateResult,
        context: Context
    ) {
        flowInterrupted = true
        state.stateStatus = FlowStateStatus.CANCELLED
        state.result = Gson().toJson(assessmentStateResult)
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                assessmentsRepository.abandonFlow(state)
            }
            moveToResults(context)
        }
    }

    fun moveToResults(ctx: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val scoreCards = assessmentsRepository.getResultsForScoreCard()
            assessmentsRepository.convertStatesToSubmissions()
            withContext(Dispatchers.Main) {
                val i = Intent(ctx, StudentScoreboardActivity::class.java)
                val json = Gson().toJson(scoreCards)
                i.putExtra(StudentScoreboardActivity.SCORECARD_LIST, json)
                i.putExtra(AssessmentConstants.KEY_SCHOOL_DATA, schoolData)
                i.putExtra(AssessmentConstants.KEY_STUDENT_ID, studentId)
                i.putExtra(AssessmentConstants.KEY_GRADE, grade)
                backgroundWorkCompleted.postValue(true)
                ctx.startActivity(i)
            }
        }
    }

    //FIX FOR: https://console.firebase.google.com/project/mission-prerna/crashlytics/app/android:org.samagra.missionPrerna/issues/f7ebd247b1fba7238617fd9372da330a
    fun cachedAssessmentStateDetails(): AssessmentStateDetails? {
        Timber.d("cachedAssessmentStateDetails: pull from viewmodel")
        return if (this::cachedAssessmentState.isInitialized) {
            cachedAssessmentState
        } else {
            Timber.e("cachedAssessmentStateDetails cachedAssessment is null in vm")
            null
        }
    }
}