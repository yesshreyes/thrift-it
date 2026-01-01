package com.example.thriftit.presentation.util

import com.example.thriftit.domain.models.Item

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

    // Convenience properties
    val isIdle: Boolean get() = this is Idle
    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error

    // Get data safely
    fun getDataOrNull(): T? = (this as? Success)?.data

    // Transform success data
    inline fun <R> map(transform: (T) -> R): UiState<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Error -> Error(message, throwable)
            is Loading -> Loading
            is Idle -> Idle
        }
}

// Screen-specific UI states
sealed class AuthUiState {
    data object Idle : AuthUiState()

    data object Loading : AuthUiState()

    data class PhoneVerificationSent(
        val verificationId: String,
    ) : AuthUiState()

    data class Success(
        val userId: String,
    ) : AuthUiState()

    data class Error(
        val message: String,
    ) : AuthUiState()
}

sealed class ItemListUiState {
    data object Loading : ItemListUiState()

    data class Success(
        val items: Item,
        val hasMore: Boolean = false,
    ) : ItemListUiState()

    data class Error(
        val message: String,
    ) : ItemListUiState()

    data object Empty : ItemListUiState()
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
