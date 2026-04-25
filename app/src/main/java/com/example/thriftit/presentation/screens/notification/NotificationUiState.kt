package com.example.thriftit.presentation.screens.notification

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val type: NotificationType,
)

enum class NotificationType {
    ITEM_UPLOADED,
    NEW_ITEM_NEARBY,
    GENERAL,
}

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
)
