package com.samagra.parent.ui.assessmenthome

import android.app.Application
import android.content.Context
import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.data.db.DbHelper
import com.data.db.NLDatabase
import com.data.repository.MetadataRepository
import com.data.repository.StudentsRepository
import com.posthog.android.PostHog
import com.samagra.ancillaryscreens.data.prefs.CommonsPrefsHelperImpl
import com.samagra.ancillaryscreens.di.FormManagementCommunicator
import com.samagra.commons.MetaDataExtensions
import com.samagra.commons.basemvvm.BaseViewModel
import com.samagra.commons.basemvvm.SingleLiveEvent
import com.samagra.commons.constants.Constants
import com.samagra.commons.models.Result
import com.samagra.grove.logging.Grove
import com.samagra.parent.helper.MentorDataHelper
import com.samagra.parent.helper.MetaDataHelper
import com.samagra.parent.helper.RealmStoreHelper
import com.samagra.parent.helper.SyncRepository
import com.samagra.parent.helper.SyncingHelper
import com.samagra.parent.repository.TeacherPerformanceInsightsRepository
import com.samagra.parent.ui.DataSyncRepository
import com.samagra.parent.ui.assessmenthome.states.TeacherInsightsStates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AssessmentHomeVM @Inject constructor(
    application: Application,
    private val dataSyncRepo: DataSyncRepository,
    val teacherPerformanceInsightsRepository: TeacherPerformanceInsightsRepository,
    val nlDatabase: NLDatabase?
) : BaseViewModel(application) {

    @Inject
    lateinit var metaRepo: MetadataRepository

    @Inject
    lateinit var studentsRepository: StudentsRepository

    val gotoLogin = MutableLiveData<Unit>()
    val logoutUserLiveData = MutableLiveData<Unit>()
    val showSyncBeforeLogout = MutableLiveData<Unit>()
    val nameValue = ObservableField("")
    val designation = ObservableField("")
    val udise = MutableLiveData("")
    val updateSync = MutableLiveData<Int>()
    val phoneNumberValue = ObservableField("")
    val mentorDetailsSuccess = MutableLiveData<Result>()
    val mentorOverViewData = MutableLiveData<HomeOverviewData>()
    val setupNewAssessmentClicked = SingleLiveEvent<Unit>()
    val helpFaqList = SingleLiveEvent<String>()
    val helpFaqFormUrl = SingleLiveEvent<String>()
    val syncRequiredLiveData = MutableLiveData<Boolean>()
    private val syncRepo = SyncRepository()

    private val teacherInsightsMutableState: MutableStateFlow<TeacherInsightsStates> =
        MutableStateFlow(TeacherInsightsStates.Loading)

    val teacherInsightsState: StateFlow<TeacherInsightsStates> =
        teacherInsightsMutableState.asStateFlow()

    fun fetchTeacherPerformanceInsights(udise: String) {
        viewModelScope.launch {
            try {
                teacherInsightsMutableState.value = TeacherInsightsStates.Loading
                val teacherPerformanceInsightsResult =
                    teacherPerformanceInsightsRepository.fetchTeacherPerformanceInsights(udise)
                when (teacherPerformanceInsightsResult) {
                    is com.data.network.Result.Success -> {
                        teacherInsightsMutableState.value =
                            TeacherInsightsStates.Success(teacherPerformanceInsightsResult.data)
                    }

                    else -> {
                        teacherInsightsMutableState.value =
                            TeacherInsightsStates.Error(Exception("An error occurred"))
                    }
                }
            } catch (t: Throwable) {
                teacherInsightsMutableState.value = TeacherInsightsStates.Error(t)
            }
        }
    }

    fun getTeacherPerformanceInsights() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                teacherPerformanceInsightsRepository.getTeacherPerformanceInsights()
                    .collect { insights ->
                        if (insights.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                teacherInsightsMutableState.value =
                                    TeacherInsightsStates.Success(insights)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                teacherInsightsMutableState.value =
                                    TeacherInsightsStates.Error(Exception(""))
                                Timber.i("No data in db")
                            }
                        }
                    }
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    teacherInsightsMutableState.value = TeacherInsightsStates.Error(t)
                }
            }
        }
    }

    fun onSetupNewAssessmentClicked() {
        setupNewAssessmentClicked.call()
    }

    private fun downLoadWorkflowConfig() {
        dataSyncRepo.downloadWorkFlowConfigFromRemoteConfig()
    }

    private fun downloadOdkFormLength(prefs: CommonsPrefsHelperImpl) {
        dataSyncRepo.downloadFormsLength(prefs)
    }

    fun downloadDataFromRemoteConfig(prefs: CommonsPrefsHelperImpl, internetAvailable: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            downLoadWorkflowConfig()
            downloadOdkFormLength(prefs)
        }
    }

    fun onLogoutClicked() {
        CoroutineScope(Dispatchers.IO).launch {

            if (RealmStoreHelper.getFinalResults()
                    .isNotEmpty() || RealmStoreHelper.getSurveyResults()
                    .isNotEmpty() || DbHelper.isSyncingRequired()
            ) {
                showSyncBeforeLogout.postValue(Unit)
            } else {
                logoutUserLiveData.postValue(Unit)
            }
        }
    }

    fun onLogoutUserData(
        prefs: CommonsPrefsHelperImpl
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            progressBarVisibility.postValue(true)
            withContext(Dispatchers.Main) {
                FormManagementCommunicator.getContract().resetEverythingODK(
                    getApplication() as Context
                ) { failedResetActions ->
                    Grove.d("Failure to reset actions at Assessment Home screen $failedResetActions")
                    viewModelScope.launch(Dispatchers.IO) {
                        clearAllUserData(prefs)
                        delay(500)
                        progressBarVisibility.postValue(false)
                        gotoLogin.postValue(Unit)
                    }
                }
            }
        }
    }

    private suspend fun clearRealmTables() {
        val isSuccess = withContext(Dispatchers.IO) {
            RealmStoreHelper.clearAllTables()
        }
        Timber.d("clearRealmTables: ")
    }

    private suspend fun clearAllUserData(prefs: CommonsPrefsHelperImpl) {
        prefs.clearData()
        clearRealmTables()
        nlDatabase?.clearAllTables()
        PostHog.with(getApplication()).reset()
    }

    fun getHelpFaqList() {
        helpFaqList.value = dataSyncRepo.getHelpFaqListFromFirebase()
    }

    fun getHelpFaqFormUrl() {
        helpFaqFormUrl.value = dataSyncRepo.getHelpFaqFormUrlFromFirebase()
    }

    fun syncDataToServer(
        prefs: CommonsPrefsHelperImpl, success: () -> Unit, failure: () -> Unit
    ) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                progressBarVisibility.postValue(true)
                Timber.i("In Progress @ " + Date())
                val helper = SyncingHelper()
                var isSuccess = helper.syncAssessments(prefs)
                isSuccess = helper.syncSubmissions(prefs) && isSuccess
                isSuccess = helper.syncSurveys(prefs) && isSuccess
                Timber.i("IsSuccess : $isSuccess")
                withContext(Dispatchers.Main) {
                    if (isSuccess) success() else failure()
                    progressBarVisibility.postValue(false)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncDataFromServer(prefs: CommonsPrefsHelperImpl, enforce: Boolean = false) {
        Timber.d("syncDataFromServer: ")
        viewModelScope.launch(Dispatchers.IO) {
            progressBarVisibility.postValue(true)
            MentorDataHelper.fetchMentorData(enforce, prefs).collect {
                Timber.d("syncDataFromServer: collect $it")
                if (it == null) return@collect
                getMentorDetailsFromPrefs(prefs)
                if (prefs.selectedUser.equals(Constants.USER_TEACHER).not()) {
                    getOverviewDataFormPrefs(prefs)
                }
                progressBarVisibility.postValue(false)
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            MetaDataHelper.fetchMetaData(
                prefs = prefs,
                enforce = enforce
            ).collect {
                Timber.d("syncDataFromServer metadata collect: $it")
            }
        }
    }

    private fun getMentorDetailsFromPrefs(prefs: CommonsPrefsHelperImpl) {
        val mentorDetails = prefs.mentorDetailsData
        mentorDetails?.let {
            mentorDetailsSuccess.postValue(it)
            nameValue.set(it.officer_name)
            phoneNumberValue.set(it.phone_no)
            val designation = MetaDataExtensions.getDesignationFromId(
                it.designation_id, prefs.designationsListJson
            )
            this.designation.set(designation)
            this.udise.postValue(it.udise.toString())
        }
    }

    private suspend fun getOverviewDataFormPrefs(prefs: CommonsPrefsHelperImpl) {
        val overviewDataFromPrefs =
            MentorDataHelper.getOverviewDataFromPrefs(prefs.mentorOverviewDetails)
        overviewDataFromPrefs?.let { overview ->
            val finalResultsRealm = RealmStoreHelper.getFinalResults()
            val homeOverviewData = if (finalResultsRealm.isNotEmpty()) {
                MentorDataHelper.setOverviewCalculations(finalResultsRealm, overview)
            } else {
                overview
            }
            mentorOverViewData.postValue(homeOverviewData)
        }
    }

    fun checkForFallback(prefs: CommonsPrefsHelperImpl) {
        syncRepo.syncToServer(prefs) {
            // Handle Loader if required
        }
    }

    fun updateOfflineData() {
        viewModelScope.launch(Dispatchers.IO) {
            val areCompetenciesAvailable = metaRepo.areCompetenciesAvailable()
            syncRequiredLiveData.postValue(areCompetenciesAvailable.not())
            studentsRepository.addDummyStudents()
        }
    }
}