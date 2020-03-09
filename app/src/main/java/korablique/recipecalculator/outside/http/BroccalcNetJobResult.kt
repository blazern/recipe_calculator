package korablique.recipecalculator.outside.http

import korablique.recipecalculator.outside.ServerErrorException
import korablique.recipecalculator.outside.ServerErrorResponse
import java.io.IOException
import java.lang.IllegalStateException

sealed class BroccalcNetJobResult<T:Any> {
    data class Ok<T:Any>(val item: T) : BroccalcNetJobResult<T>()
    sealed class Error<T:Any> : BroccalcNetJobResult<T>() {
        sealed class ServerError<T:Any> : Error<T>() {
            data class NotLoggedIn<T:Any>(val e: ServerErrorResponse?) : ServerError<T>()
            data class Other<T:Any>(val e: ServerErrorResponse) : ServerError<T>()
        }
        data class NetError<T:Any>(val e: IOException) : Error<T>()
        data class ResponseFormatError<T:Any>(val e: Throwable) : Error<T>()
        data class OtherError<T:Any>(val e: Throwable) : Error<T>()
    }
}

fun <T:Any> BroccalcNetJobResult<T>.unwrapException(): Throwable {
    return when (this) {
        is BroccalcNetJobResult.Ok -> throw IllegalStateException("No exception in OK")
        is BroccalcNetJobResult.Error.ServerError.NotLoggedIn -> {
            if (this.e != null) {
                ServerErrorException(this.e)
            } else {
                RuntimeException("Artificial exception for NotLoggedIn error")
            }
        }
        is BroccalcNetJobResult.Error.ServerError.Other -> ServerErrorException(this.e)
        is BroccalcNetJobResult.Error.ResponseFormatError -> this.e
        is BroccalcNetJobResult.Error.NetError -> this.e
        is BroccalcNetJobResult.Error.OtherError -> this.e
    }
}

fun <T:Any> BroccalcNetJobResult<T>.tryGetErrorStatus(): String? {
    return if (this is BroccalcNetJobResult.Error.ServerError) {
        when (this) {
            is BroccalcNetJobResult.Error.ServerError.NotLoggedIn -> {
                this.e?.status
            }
            is BroccalcNetJobResult.Error.ServerError.Other -> {
                this.e.status
            }
        }
    } else {
        null
    }
}