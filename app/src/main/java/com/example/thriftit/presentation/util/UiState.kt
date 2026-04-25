package com.example.thriftit.presentation.util

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()

    data object Loading : UiState<Nothing>()

    data class Success<T>(
        val data: T,
    ) : UiState<T>()

    data class Error(
        val message: String,
        val throwable: Throwable? = null,
    ) : UiState<Nothing>()

    val isIdle: Boolean get() = this is Idle
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    fun getDataOrNull(): T? = (this as? Success)?.data

    inline fun <R> map(transform: (T) -> R): UiState<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(message, throwable)
            is Loading -> Loading
            is Idle -> Idle
        }
}

sealed class UploadUiState {
    data object Idle : UploadUiState()

    data class Uploading(
        val progress: Float,
    ) : UploadUiState()

    data object Success : UploadUiState()

    data class Error(
        val message: String,
    ) : UploadUiState()
}
