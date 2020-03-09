package korablique.recipecalculator.outside.userparams

import korablique.recipecalculator.base.BaseActivity
import korablique.recipecalculator.base.executors.IOExecutor
import korablique.recipecalculator.base.executors.MainThreadExecutor
import korablique.recipecalculator.base.prefs.PrefsOwner
import korablique.recipecalculator.base.prefs.SharedPrefsManager
import korablique.recipecalculator.outside.thirdparty.GPAuthResult
import korablique.recipecalculator.outside.thirdparty.GPAuthorizer
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import korablique.recipecalculator.model.UserNameProvider
import korablique.recipecalculator.outside.STATUS_ALREADY_REGISTERED
import korablique.recipecalculator.outside.ServerErrorException
import korablique.recipecalculator.outside.http.*
import java.lang.Exception

sealed class GetWithRegistrationRequestResult {
    data class Success(val user: ServerUserParams) : GetWithRegistrationRequestResult()
    data class Failure(val exception: Throwable) : GetWithRegistrationRequestResult()
    object RegistrationParamsTaken : GetWithRegistrationRequestResult()
    object CanceledByUser : GetWithRegistrationRequestResult()
}

sealed class GetWithAccountMoveRequestResult {
    data class Success(val user: ServerUserParams) : GetWithAccountMoveRequestResult()
    data class Failure(val exception: Throwable) : GetWithAccountMoveRequestResult()
    object CanceledByUser : GetWithAccountMoveRequestResult()
}

@Singleton
class ServerUserParamsRegistry @Inject constructor(
        private val mainThreadExecutor: MainThreadExecutor,
        private val ioExecutor: IOExecutor,
        private val gpAuthorizer: GPAuthorizer,
        private val userNameProvider: UserNameProvider,
        private val httpContext: BroccalcHttpContext,
        private val prefsManager: SharedPrefsManager
) {
    private val observers = mutableListOf<Observer>()
    @Volatile
    private var cachedUserParams: ServerUserParams? = null
        set(value) {
            field = value
            if (value != null) {
                // TODO: keep the vals in DB instead of preferences
                prefsManager.putString(PrefsOwner.USER_PARAMS_REGISTRY, "uid", value.uid)
                prefsManager.putString(PrefsOwner.USER_PARAMS_REGISTRY, "token", value.token)
            }
            mainThreadExecutor.execute {
                observers.forEach { it.onUserParamsChange(cachedUserParams) }
            }
        }

    interface Observer {
        fun onUserParamsChange(userParams: ServerUserParams?)
    }

    init {
        // TODO: keep the vals in DB instead of preferences
        val uid = prefsManager.getString(PrefsOwner.USER_PARAMS_REGISTRY, "uid")
        val token = prefsManager.getString(PrefsOwner.USER_PARAMS_REGISTRY, "token")
        if (token != null && uid != null) {
            val userParams = ServerUserParams(uid, token)
            cachedUserParams = userParams
        }
    }

    suspend fun getUserParamsMaybeRegister(context: BaseActivity) = withContext(ioExecutor) {
        val userParams = cachedUserParams
        if (userParams != null) {
            return@withContext GetWithRegistrationRequestResult.Success(userParams)
        }

        val gpAuthResult = gpAuthorizer.auth(context)
        when (gpAuthResult) {
            is GPAuthResult.Success -> {
                return@withContext registerWithGpToken(gpAuthResult.token)
            }
            is GPAuthResult.Failure -> {
                return@withContext GetWithRegistrationRequestResult.Failure(gpAuthResult.exception)
            }
            is GPAuthResult.CanceledByUser -> {
                return@withContext GetWithRegistrationRequestResult.CanceledByUser
            }
        }
    }

    private suspend fun registerWithGpToken(token: String): GetWithRegistrationRequestResult {
        val name = userNameProvider.userName.toString()
        val url = ("https://blazern.me/broccalc/v1/user/register?"
                + "name=$name&social_network_type=gp&social_network_token=$token")
        val response = httpContext.execute {
            val response = httpRequestAndUnwrap(url, RegisterResponse::class)
            val userParams = ServerUserParams(response.user_id, response.client_token)
            cachedUserParams = userParams
            BroccalcNetJobResult.Ok(userParams)
        }

        when (response) {
            is BroccalcNetJobResult.Ok -> {
                return GetWithRegistrationRequestResult.Success(response.item)
            }
            is BroccalcNetJobResult.Error -> {
                if (STATUS_ALREADY_REGISTERED == response.tryGetErrorStatus()) {
                    return GetWithRegistrationRequestResult.RegistrationParamsTaken
                }
                return GetWithRegistrationRequestResult.Failure(response.unwrapException())
            }
        }
    }

    suspend fun getUserParamsMaybeMoveAccount(context: BaseActivity) = withContext(ioExecutor) {
        val cachedUserParams = cachedUserParams
        if (cachedUserParams != null) {
            return@withContext GetWithAccountMoveRequestResult.Success(cachedUserParams)
        }

        val gpAuthResult = gpAuthorizer.auth(context)
        when (gpAuthResult) {
            is GPAuthResult.Success -> {
                return@withContext moveAccountWithGpToken(gpAuthResult.token)
            }
            is GPAuthResult.Failure -> {
                return@withContext GetWithAccountMoveRequestResult.Failure(gpAuthResult.exception)
            }
            is GPAuthResult.CanceledByUser -> {
                return@withContext GetWithAccountMoveRequestResult.CanceledByUser
            }
        }
    }

    private suspend fun moveAccountWithGpToken(token: String): GetWithAccountMoveRequestResult {
        val url = ("https://blazern.me/broccalc/v1/user/move_device_account?"
                + "social_network_type=gp&social_network_token=$token")
        val response = httpContext.execute {
            val response = httpRequestAndUnwrap(url, MoveDeviceAccountResponse::class)
            val userParams = ServerUserParams(response.user_id, response.client_token)
            cachedUserParams = userParams
            BroccalcNetJobResult.Ok(userParams)
        }

        when (response) {
            is BroccalcNetJobResult.Ok -> {
                return GetWithAccountMoveRequestResult.Success(response.item)
            }
            is BroccalcNetJobResult.Error -> {
                return GetWithAccountMoveRequestResult.Failure(response.unwrapException())
            }
        }
    }

    fun getUserParams() = cachedUserParams

    fun addObserver(observer: Observer) {
        observers += observer
    }

    fun removeObserver(observer: Observer) {
        observers -= observer
    }
}