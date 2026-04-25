package com.example.thriftit.domain.util

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()

    data class Error(
        val exception: Exception,
        val message: String = exception.message ?: "An unknown error occurred",
    ) : Result<Nothing>()

    data object Loading : Result<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = if (this is Success) data else null

    inline fun <R> map(transform: (T) -> R): Result<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(exception, message)
            is Loading -> Loading
        }
}

suspend inline fun <T> resultOf(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e)
    }

