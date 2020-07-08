package korablique.recipecalculator.outside.fcm

import androidx.annotation.WorkerThread
import com.google.firebase.messaging.RemoteMessage
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.base.logging.Log
import korablique.recipecalculator.dagger.InjectorHolder
import javax.inject.Inject

class FCMService : com.google.firebase.messaging.FirebaseMessagingService() {
    @Inject
    lateinit var fcmManager: FCMManager
    @Inject
    lateinit var mainThreadExecutor: MainThreadExecutor

    override fun onCreate() {
        super.onCreate()
        InjectorHolder.getInjector().inject(this)
    }

    @WorkerThread
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i("FCMService.onNewToken, token: $token")
        mainThreadExecutor.execute {
            fcmManager.onFCMTokenChanged(token)
        }
    }

    @WorkerThread
    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)
        Log.i("FCMService.onMessageReceived, msg: ${msg.data}")
        mainThreadExecutor.execute {
            fcmManager.onMessageReceived(msg.data)
        }
    }
}