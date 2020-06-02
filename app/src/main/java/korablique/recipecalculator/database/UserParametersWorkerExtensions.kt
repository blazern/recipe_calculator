package korablique.recipecalculator.database

import korablique.recipecalculator.model.UserParameters
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun UserParametersWorker.getCurrentUserParametersKx(): UserParameters?
        = suspendCoroutine { continuation ->
    val single = requestCurrentUserParameters()
    single.subscribe { optionalParams ->
        val result = if (optionalParams.isPresent) {
            optionalParams.get()
        } else {
            null
        }
        continuation.resume(result)
    }
}