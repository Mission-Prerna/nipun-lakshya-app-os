package com.samagra.parent.ui.assessmenthome

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.assessment.schoolhistory.SchoolHistoryActivity
import com.assessment.studentselection.StudentSelectionActivity
import com.chatbot.BotIconState
import com.chatbot.ChatBotActivity
import com.chatbot.ChatBotVM
import com.google.android.gms.location.*
import com.samagra.ancillaryscreens.data.prefs.CommonsPrefsHelperImpl
import com.samagra.ancillaryscreens.utils.observe
import com.samagra.commons.CompositeDisposableHelper
import com.samagra.commons.MetaDataExtensions
import com.samagra.commons.basemvvm.BaseFragment
import com.samagra.commons.constants.Constants
import com.samagra.commons.models.Result
import com.samagra.commons.models.schoolsresponsedata.SchoolsData
import com.samagra.commons.posthog.*
import com.samagra.commons.posthog.data.Cdata
import com.samagra.commons.posthog.data.Edata
import com.samagra.commons.posthog.data.Object
import com.samagra.grove.logging.Grove
import com.samagra.parent.*
import com.samagra.parent.R
import com.samagra.parent.authentication.AuthenticationActivity
import com.samagra.parent.databinding.FragmentAssessmentTeacherHomeBinding
import com.samagra.parent.ui.*
import com.samagra.parent.ui.assessmenthome.states.TeacherInsightsStates
import com.samagra.parent.ui.logout.LogoutUI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_assessment_teacher_home.*
import org.odk.collect.android.utilities.ToastUtils
import java.util.*

@AndroidEntryPoint
class AssessmentTeacherHomeFragment :
    BaseFragment<FragmentAssessmentTeacherHomeBinding, AssessmentHomeVM>() {

    @LayoutRes
    override fun layoutId() = R.layout.fragment_assessment_teacher_home

    private var schoolsData: SchoolsData? = null
    private var dialogShowing: Boolean = false
    private var dialogBuilder: AlertDialog? = null
    private val prefs: CommonsPrefsHelperImpl by lazy { initPreferences() }
    private lateinit var insightsRecyclerView: RecyclerView
    private lateinit var insightsAdapter: TeacherInsightsAdapter

    private val chatVM by viewModels<ChatBotVM>()

    override fun getBaseViewModel(): AssessmentHomeVM {
        val hiltViewModel: AssessmentHomeVM by activityViewModels()
        return hiltViewModel
    }

    override fun getBindingVariable() = BR.assessmentHomeVm

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getDataFromArgument()
        setObservers()
        setupChatBotFlow()
        setupOverViewUI()
        setListeners()
        initRecyclerView()
        viewModel.updateOfflineData()
    }

    private fun initRecyclerView(){
        insightsRecyclerView = binding.recyclerView
        insightsAdapter = TeacherInsightsAdapter()
        insightsRecyclerView.adapter = insightsAdapter
        insightsRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun setupChatBotFlow() {
        chatVM.identifyChatIconState()
    }

    private fun getDataFromArgument() {
        schoolsData = arguments?.getSerializable(AppConstants.INTENT_SCHOOL_DATA) as SchoolsData
        if (UtilityFunctions.isNetworkConnected(context)) {
            viewModel.getTeacherPerformanceInsights()
            viewModel.fetchTeacherPerformanceInsights(schoolsData?.udise.toString())
        } else {
            viewModel.getTeacherPerformanceInsights()
        }
    }

    private fun setBlockVisibility(visibility: Int) {
        binding.groupBlock.visibility = visibility
    }

    private fun callApis(enforce: Boolean) {
        viewModel.downloadDataFromRemoteConfig(prefs, UtilityFunctions.isInternetAvailable(context))
        viewModel.syncDataFromServer(prefs, enforce)
        viewModel.checkForFallback(prefs)
    }

    private fun setListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            callApis(enforce = true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (dialogShowing) {
            showSyncAlertDialog()
        }
        setSyncButtonUI()
    }

    private fun showSyncAlertDialog() {
        dialogBuilder?.let {
            if (it.isShowing) {
                return@let
            } else {
                it.show()
            }
        } ?: run {
            dialogBuilder =
                AlertDialog.Builder(context!!).setMessage(getString(R.string.data_sync_successful))
                    .setPositiveButton(getText(R.string.ok)) { dialog, _ ->
                        dialog.dismiss()
                    }.show()
        }
    }

    private fun setSyncButtonUI() {
        binding.mtlBtnSetupAssessment.visibility = View.VISIBLE
        binding.mtlBtnSetupAssessment.minLines = 2
        binding.mtlBtnSetupAssessment.setOnClickListener {
            sendStartAssessmentEvent()
            redirectToStudentSelectionScreen()
        }
        binding.mtlBtnSchoolAssessmentSummary.setOnClickListener {
            sendSchoolHistoryEvent()
            redirectToSchoolHistoryScreen()
        }
    }

    private fun setObservers() {
        with(viewModel) {
            observe(mentorDetailsSuccess, ::handleMentorDetails)
            observe(updateSync, ::handleSyncFlow)
            observe(failure, ::handleFailure)
            observe(showToastResWithId, ::handleMessage)
            observe(showSyncBeforeLogout, ::handleSyncBeforeLogout)
            observe(logoutUserLiveData, ::handleLogoutUser)
            observe(gotoLogin, ::handleLogoutRedirection)
            observe(progressBarVisibility, ::handleProgressBarVisibility)
        }

        lifecycleScope.launchWhenStarted {
            viewModel.teacherInsightsState.collect {
                when (it) {
                    is TeacherInsightsStates.Loading -> {
                        showProgressBar()
                    }

                    is TeacherInsightsStates.Error -> {
                        hideProgressBar()
                        handleFailure(it.t.message)
                    }

                    is TeacherInsightsStates.Success -> {
                        hideProgressBar()
                        insightsAdapter.updateItems(it.teacherInsightsStatesInfo)
                    }
                }
            }
        }
        chatVM.iconVisibilityLiveData.observe(this, ::handeIconVisibilityState)

        lifecycleScope.launchWhenStarted {
            viewModel.syncRequiredLiveData.observe(this@AssessmentTeacherHomeFragment) {
                callApis(enforce = it)
            }
        }
    }

    private fun handeIconVisibilityState(state: BotIconState?) {
        when (state) {
            BotIconState.Hide -> {
                binding.botFab.visibility = View.GONE
            }

            is BotIconState.Show -> {
                showChatbot(
                    animate = state.animate,
                    botView = binding.botFab,
                    botIconView = binding.botIcon,
                    imageIconRes = R.drawable.bot,
                    animationGifRes = R.drawable.animate_bot,
                    intentOnClick = Intent(context, ChatBotActivity::class.java)
                )
            }

            null -> {
                //IGNORE
            }
        }
    }

    private fun handleLogoutRedirection(@Suppress("UNUSED_PARAMETER") unit: Unit?) {
        setRedirectionsOnIntent()
    }

    private fun handleLogoutUser(@Suppress("UNUSED_PARAMETER") unit: Unit?) {
        LogoutUI.confirmLogout(context) {
            viewModel.onLogoutUserData(prefs)
        }
    }

    private fun handleSyncBeforeLogout(@Suppress("UNUSED_PARAMETER") unit: Unit?) {
        confirmLogoutWithSync()
    }

    private fun confirmLogoutWithSync() {
        LogoutUI.confirmLogoutWithSync(context!!) {
            viewModel.syncDataToServer(prefs, {
                viewModel.onLogoutUserData(prefs)
            }) {
                ToastUtils.showShortToast(R.string.error_generic_message)
            }
        }
    }

    private fun handleSyncFlow(msg: Int?) {
        msg?.let {
            ToastUtils.showShortToast(it)
        }
        setSyncButtonUI()
    }

    override fun onPause() {
        super.onPause()
        dialogBuilder?.let {
            if (it.isShowing) {
                dialogShowing = true
                it.dismiss()
            }
        }
    }
    private fun handleMessage(textResId: Int?) {
        textResId?.let {
            ToastUtils.showShortToast(it)
        }
    }

    private fun initPreferences() = CommonsPrefsHelperImpl(context, "prefs")

    private fun setupOverViewUI() {
        binding.clProfileOverview.visibility = View.VISIBLE
        binding.clOverview.visibility = View.VISIBLE
        binding.titleMentorDetails.text = getString(R.string.teacher_profile)
    }

    private fun redirectToStudentSelectionScreen(){
        val intent = Intent(context, StudentSelectionActivity::class.java)
        if (schoolsData != null) {
            intent.putExtra(AppConstants.INTENT_SCHOOL_DATA, schoolsData)
        } else {
            Grove.e("Schools data is null and selected user is: ${prefs.selectedUser}")
        }
        startActivity(intent)
    }

    private fun redirectToSchoolHistoryScreen(){
        val intent = Intent(context, SchoolHistoryActivity::class.java)
        if (schoolsData != null) {
            intent.putExtra(AppConstants.INTENT_SCHOOL_DATA, schoolsData)
        } else {
            Grove.e("Schools data is null and selected user is: ${prefs.selectedUser}")
        }
        startActivity(intent)
    }

    private fun sendStartAssessmentEvent() {
        val list = ArrayList<Cdata>()
        val mentorDetailsFromPrefs = prefs.mentorDetailsData
        mentorDetailsFromPrefs?.let {
            list.add(Cdata("userId", "" + it.id))
            list.add(Cdata("userType", "" + it.actorId))
            list.add(Cdata("schoolId", "" + it.schoolId))
            list.add(Cdata("districtName", "" + it.district_name))
            list.add(Cdata("blockName", "" + it.block_town_name))
        }
        val properties = PostHogManager.createProperties(
            page = DASHBOARD_SCREEN,
            eventType = EVENT_TYPE_USER_ACTION,
            eid = EID_INTERACT,
            context = PostHogManager.createContext(APP_ID, NL_APP_DASHBOARD, list),
            eData = Edata(NL_DASHBOARD, TYPE_CLICK),
            objectData = Object.Builder().id(SETUP_ASSESSMENT_BUTTON).type(OBJ_TYPE_UI_ELEMENT)
                .build(),
            prefs = PreferenceManager.getDefaultSharedPreferences(context)
        )
        PostHogManager.capture(activity!!, EVENT_HOME_SCREEN_START_ASSESSMENT, properties)
    }
    private fun sendSchoolHistoryEvent() {
        val list = ArrayList<Cdata>()
        val mentorDetailsFromPrefs = prefs.mentorDetailsData
        mentorDetailsFromPrefs?.let {
            list.add(Cdata("userId", "" + it.id))
        }
        val properties = PostHogManager.createProperties(
            page = DASHBOARD_SCREEN,
            eventType = EVENT_TYPE_USER_ACTION,
            eid = EID_INTERACT,
            context = PostHogManager.createContext(APP_ID, NL_APP_DASHBOARD, list),
            eData = Edata(NL_DASHBOARD, TYPE_CLICK),
            objectData = Object.Builder().id(SETUP_ASSESSMENT_BUTTON).type(OBJ_TYPE_UI_ELEMENT)
                .build(),
            prefs = PreferenceManager.getDefaultSharedPreferences(context)
        )
        PostHogManager.capture(activity!!, EVENT_HOME_SCREEN_SCHOOL_HISTORY, properties)
    }

    private fun handleFailure(errorMessage: String?) {
        ToastUtils.showShortToast(errorMessage)
    }

    private fun handleMentorDetails(result: Result?) {
        val designation =
            MetaDataExtensions.getDesignationFromId(
                result?.designation_id ?: 0,
                prefs.designationsListJson
            )
        result?.let {
            Grove.e("user id mentors ${it.id}")
            if (designation.equals(Constants.USER_DESIGNATION_SRG, true)) {
                setBlockVisibility(View.GONE)
            } else {
                setBlockVisibility(View.VISIBLE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        CompositeDisposableHelper.destroyCompositeDisposable()
    }

    private fun setRedirectionsOnIntent() {
        val intentToUserSelection = Intent(context, AuthenticationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intentToUserSelection)
        activity!!.finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        onBackPressed()
    }

    private fun handleProgressBarVisibility(visible: Boolean?) {
        if (visible == true) {
            showProgressBar()
        } else {
            binding.swipeRefresh.isRefreshing = false
            hideProgressBar()
        }
    }

    companion object {
        fun newInstance(schoolsData: SchoolsData?): AssessmentTeacherHomeFragment =
            AssessmentTeacherHomeFragment().withArgs {
                putSerializable(AppConstants.INTENT_SCHOOL_DATA, schoolsData)
            }
    }
}