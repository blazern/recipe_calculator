package korablique.recipecalculator.outside.fcm

import com.google.firebase.iid.FirebaseInstanceId
import korablique.recipecalculator.base.logging.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
open class FCMTokenObtainer @Inject constructor() {
    open suspend fun requestToken(): String? = suspendCoroutine { continuation ->
        Log.i("FCMTokenObtainer.requestToken start")
        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.i("FCMTokenObtainer.requestToken failure: ${task.exception}")
                        continuation.resume(null)
                        return@addOnCompleteListener
                    }

                    Log.i("FCMTokenObtainer.requestToken success")
                    continuation.resume(task.result?.token)
                }
    }
}