package com.chatbot

import android.content.Context
import android.os.Environment
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chatbot.model.ChatbotTelemetryAction
import com.chatbot.notification.ChatbotNotificationHandler
import com.chatbot.notification.NotificationTelemetryType
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.samagra.commons.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


class ChatBotVM : ViewModel() {

    val botsLiveData = MutableLiveData<ChatbotListingState>()

    private val iconVisibilityMutableLiveData = MutableLiveData<BotIconState>()
    val iconVisibilityLiveData: LiveData<BotIconState> = iconVisibilityMutableLiveData

    val nlFolder = "NipunLakshya"
    private val chatbotBaseUrl = "file:///android_asset/chatbot/"
    val chatbotIndexUrl = "${chatbotBaseUrl}index.html"

    val gson by lazy { Gson() }

    fun submitChat(chatId: String, chat: String) = AppPreferences.submitChat(chatId, chat)

    fun saveStarredMessages(savedMsg: String) = AppPreferences.saveStarredMessages(savedMsg)

    fun getInjection(
        botToFocus: String,
        userConfiguredBots: List<String>,
        userStartedBots: List<String>
    ): String {
        val injectionBuilder = StringBuilder()
        val unstartedBots = mutableListOf<String>()
        if (userStartedBots.isNotEmpty()) {
            unstartedBots.addAll(userConfiguredBots.minus(userStartedBots.toSet()))
        }
        Timber.d("getInjection: configured: $userConfiguredBots")
        Timber.d("getInjection: started: $userStartedBots")
        Timber.d("getInjection: unstarted: $unstartedBots")
        injectionBuilder.append(
            "<script type='text/javascript'>" +
                    "localStorage.setItem('auth', '${AppPreferences.getUserAuth()}' );" +
                    "localStorage.setItem('mobile', '${AppPreferences.getUserMobile()}');" +
                    "localStorage.setItem('botList','${gson.toJson(userConfiguredBots)}' );" +
                    "localStorage.setItem('unstartedBotList','${gson.toJson(unstartedBots)}' );" +
                    "localStorage.setItem('botDetails', '${AppPreferences.getChatBotDetails()}');" +
                    "localStorage.setItem('starredChats', '${AppPreferences.getStarredMsgs()}');" +
                    "localStorage.setItem('chatHistory', '${AppPreferences.getChatHistory()}' );"
        )

        if (botToFocus.isNotEmpty())
            injectionBuilder.append("localStorage.setItem('botToFocus', '$botToFocus' );")

        ChatBotRepository.getChatbotUrls().forEach {
            injectionBuilder.append("localStorage.setItem('${it.key}', '${it.value}' );")
        }

        injectionBuilder.append(
            "window.location.replace('${chatbotIndexUrl}');" +
                    "</script>"
        )
        return injectionBuilder.toString()
    }

    fun saveBotDetails(botDetailsJson: String) =
        AppPreferences.saveChatBotDetails(botDetailsJson)

    fun clearLocalStorage() = AppPreferences.clearLocal()

    fun fetchConfiguredBots() {
        viewModelScope.launch {
            try {
                val userPhone = AppPreferences.getUserMobile()
                ChatBotRepository.fetchMentorBots(userPhone, { userConfiguredBots ->
                    viewModelScope.launch {
                        val userStartedBots = getUserStartedBots(backupBots = userConfiguredBots)
                        botsLiveData.postValue(
                            ChatbotListingState.Success(
                                userConfiguredBots = userConfiguredBots,
                                userStartedBots = userStartedBots
                            )
                        )
                    }
                }, {
                    val botList = cachedChatBotList()
                    botsLiveData.postValue(
                        ChatbotListingState.Success(
                            userConfiguredBots = botList,
                            userStartedBots = botList
                        )
                    )
                })
            } catch (e: Exception) {
                Timber.e(e)
                botsLiveData.postValue(
                    ChatbotListingState.Failure(
                        reason = R.string.failed_load_bot,
                        destructive = true
                    )
                )
            }
        }
    }

    private suspend fun getUserStartedBots(backupBots: List<String>): List<String> {
        return try {
            val startedBots =
                ChatBotRepository.getChatbotsWithAction(ChatbotTelemetryAction.STARTED)
            if (startedBots.isNullOrEmpty()) {
                if (startedBots?.isEmpty() == true) {
                    //If no bots are started yet, this is the first launch for the user.
                    // Mark all bots as started
                    ChatBotRepository.setBotsWithAction(
                        botIds = backupBots,
                        action = ChatbotTelemetryAction.STARTED
                    )
                }
                backupBots
            } else {
                startedBots
            }
        } catch (t: Throwable) {
            Timber.e(t, "getUserStartedBots: ${t.message}")
            backupBots
        }
    }

    fun logNotificationReadTelemetry(context: Context, data: Map<String, String>) {
        ChatbotNotificationHandler.triggerNotificationTelemetry(
            context = context,
            type = NotificationTelemetryType.READ,
            messageData = data
        )
    }

    fun getPropertiesMapFromJson(eventProperties: String?): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        if (eventProperties.isNullOrEmpty().not()) {
            val eventPropertiesObject = JsonParser.parseString(eventProperties).asJsonObject
            eventPropertiesObject.keySet().forEach {
                result[it] = eventPropertiesObject[it]
            }
        }
        result["userId"] = AppPreferences.getUserId()
        Timber.d("getPropertiesMapFromJson: map: $result")
        return result
    }

    fun getUserIdMap(): String = gson.toJson(mapOf(getUserIdPair()))
    fun getUserIdPair() = "userId" to AppPreferences.getUserId()

    fun identifyChatIconState() {
        viewModelScope.launch {
            val isChatBotVisibilityEnabled = ChatBotRepository.isChatbotEnabledForActor()
            if (isChatBotVisibilityEnabled) {
                var userBots: List<String> = emptyList()
                ChatBotRepository.fetchMentorBots(AppPreferences.getUserMobile(), {
                    userBots = it
                }, {
                    userBots = cachedChatBotList()
                })
                val userStartedBots = getUserStartedBots(backupBots = userBots)
                ChatBotRepository.setUserConfiguredBots(userBots)
                ChatBotRepository.setUserStartedBots(userStartedBots)
                iconVisibilityMutableLiveData.postValue(
                    BotIconState.Show(animate = userBots.size > userStartedBots.size)
                )
            } else {
                iconVisibilityMutableLiveData.postValue(BotIconState.Hide)
            }
        }
    }

    private fun cachedChatBotList(): List<String> = gson.fromJson(
        AppPreferences.chatBotList,
        object : TypeToken<List<String>>() {}.type
    )

    fun onConversationStarted(botId: String) {
        viewModelScope.launch {
            ChatBotRepository.setBotWithAction(
                botId = botId,
                action = ChatbotTelemetryAction.STARTED
            )
        }
    }

    fun userConfiguredBots() = ChatBotRepository.getUserConfiguredBots()
    fun userStartedBots() = ChatBotRepository.getUserStartedBots()
    fun isWebLink(url: String): Pair<Boolean, String?> {
        val webLink = url.removePrefix(chatbotBaseUrl)
        return if (webLink.contains("http")) {
            Pair(true, webLink)
        } else {
            Pair(false, null)
        }
    }

    fun downloadAsset(
        context: ChatBotActivity,
        url: String,
        type: String,
        assetId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val assetName = url.substringBefore("?").substringAfterLast("/")
            val response = ChatBotRepository.downloadAsset(url)
            if (response != null) {
                val targetFile = getDownloadedFileFromName(context, assetName)
                val fileSaved = saveFile(response, targetFile)
                if (fileSaved != null) {
                    botsLiveData.postValue(
                        ChatbotListingState.OpenAsset(
                            assetId = assetId,
                            assetName = assetName,
                            fileToOpen = fileSaved,
                            type = type
                        )
                    )
                    return@launch
                }
            }
            botsLiveData.postValue(
                ChatbotListingState.Failure(
                    reason = R.string.failed_download,
                    destructive = false
                )
            )
        }
    }

    private suspend fun saveFile(response: ResponseBody, targetFile: File): File? {
        return withContext(Dispatchers.IO) {
            try {
                response.byteStream().use {
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { targetOutputStream ->
                        it.copyTo(targetOutputStream)
                    }
                }
                targetFile
            } catch (t: Throwable) {
                t.printStackTrace()
                null
            }
        }
    }

    fun getDownloadedFileFromName(context: Context, filename: String) =
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.path
                    + "/$nlFolder/$filename"
        )
}


sealed class BotIconState {
    object Hide : BotIconState()
    class Show(val animate: Boolean) : BotIconState()
}

sealed class ChatbotListingState {

    data class Failure(@StringRes val reason: Int, val destructive: Boolean) : ChatbotListingState()

    data class Success(
        val userConfiguredBots: List<String>,
        val userStartedBots: List<String>
    ) : ChatbotListingState()

    data class OpenAsset(
        val assetId: String,
        val assetName: String,
        val fileToOpen: File,
        val type: String
    ) : ChatbotListingState()
}
