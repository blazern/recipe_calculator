package korablique.recipecalculator.outside.fcm

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.squareup.moshi.JsonClass
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.base.logging.Log
import korablique.recipecalculator.outside.http.BroccalcHttpContext
import korablique.recipecalculator.outside.http.BroccalcNetJobResult
import korablique.recipecalculator.outside.network.NetworkStateDispatcher
import korablique.recipecalculator.outside.serverAddr
import korablique.recipecalculator.outside.userparams.ServerUserParams
import korablique.recipecalculator.outside.userparams.ServerUserParamsRegistry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@VisibleForTesting
const val SERV_FIELD_MSG_TYPE = "msg_type"

@Singleton
class FCMManager @Inject constructor(
        private val context: Context,
        private val mainThreadExecutor: MainThreadExecutor,
        private val networkStateDispatcher: NetworkStateDispatcher,
        private val httpContext: BroccalcHttpContext,
        private val userParamsRegistry: ServerUserParamsRegistry,
        private val fcmTokenObtainer: FCMTokenObtainer)
    : NetworkStateDispatcher.Observer, ServerUserParamsRegistry.Observer {
    companion object {
        @VisibleForTesting
        fun createMsgForTests(type: String, otherData: Map<String, String> = emptyMap()): Map<String, String> {
            if (SERV_FIELD_MSG_TYPE in otherData) {
                throw IllegalArgumentException("$SERV_FIELD_MSG_TYPE expected not to be in $otherData")
            }
            val result = otherData.toMutableMap()
            result[SERV_FIELD_MSG_TYPE] = type
            return result
        }
    }

    private val messageReceivers = mutableMapOf<String, MessageReceiver>()
    private var sentFcmToken: String? = null
    private var sentFcmTokenOwner: String? = null

    interface MessageReceiver {
        fun onNewFcmMessage(msg: String)
    }

    init {
        networkStateDispatcher.addObserver(this)
        userParamsRegistry.addObserver(this)
        maybeAcquireTokenAndSendToServer()
    }

    fun destroy() {
        networkStateDispatcher.removeObserver(this)
        userParamsRegistry.removeObserver(this)
    }

    override fun onNetworkAvailabilityChange(available: Boolean) {
        maybeAcquireTokenAndSendToServer()
    }

    override fun onUserParamsChange(userParams: ServerUserParams?) {
        maybeAcquireTokenAndSendToServer()
    }

    internal fun onFCMTokenChanged(token: String) {
        maybeAcquireTokenAndSendToServer()
    }

    private fun maybeAcquireTokenAndSendToServer() {
        Log.i("FCMManager.maybeAcquireTokenAndSendToServer start")
        GlobalScope.launch(mainThreadExecutor) {
            val userParams = userParamsRegistry.getUserParams()
            if (userParams == null) {
                Log.i("FCMManager.maybeAcquireTokenAndSendToServer userParams == null (not logged in)")
                return@launch
            }

            val token = fcmTokenObtainer.requestToken()
            if (token == null) {
                Log.w("FCMManager.maybeAcquireTokenAndSendToServer token == null")
                return@launch
            }

            if (!networkStateDispatcher.isNetworkAvailable()) {
                Log.i("FCMManager.maybeAcquireTokenAndSendToServer no network")
                return@launch
            }

            if (token == sentFcmToken
                    && userParams.token == sentFcmTokenOwner) {
                Log.i("FCMManager.maybeAcquireTokenAndSendToServer sentFcmToken == token " +
                        "($sentFcmToken == $token)")
                return@launch
            }

            sendTokenToServer(token, userParams)
        }
    }

    private suspend fun sendTokenToServer(fcmToken: String, userParams: ServerUserParams) {
        val url = ("${serverAddr(context)}/v1/user/update_fcm_token?"
                + "client_token=${userParams.token}&user_id=${userParams.uid}&"
                + "fcm_token=$fcmToken")
        val response = httpContext.run {
            httpRequest(url, UpdateFCMTokenResponse::class)
        }
        if (response is BroccalcNetJobResult.Ok) {
            sentFcmToken = fcmToken
            sentFcmTokenOwner = userParams.token
        }
    }

    fun onMessageReceived(data: Map<String, String>) {
        val msgType = data[SERV_FIELD_MSG_TYPE]
        if (msgType == null) {
            Log.w("FCMManager.onMessageReceived: server FCM message without msg type: $data")
            return
        }
        val jsonKeyValues = data.map { """ "${it.key}":"${it.value}" """ }
        val jsonMsg = jsonKeyValues.joinToString(separator = ",\n", prefix = "{", postfix = "}")
        Log.i("FCMManager.onMessageReceived: msgType: $msgType, msg: $jsonMsg, " +
                "receiver: ${messageReceivers[msgType]}")
        messageReceivers[msgType]?.onNewFcmMessage(jsonMsg)
    }

    fun registerMessageReceiver(msgType: String, messageReceiver: MessageReceiver) {
        val displacedReceiver = messageReceivers.put(msgType, messageReceiver)
        if (displacedReceiver != null) {
            throw IllegalArgumentException("Only 1 receiver for a msg type allowed, "
                    + "second received. Type: $msgType")
        }
    }
}

@JsonClass(generateAdapter = true)
private data class UpdateFCMTokenResponse(
        val status: String
)
