package com.example.thriftit.presentation.screens.notification

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(
            NotificationUiState(
                notifications = getSampleNotifications(),
                isLoading = false,
            ),
        )
        val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()
    }

private fun getSampleNotifications(): List<Notification> =
    listOf(
        Notification(
            id = "1",
            title = "Item Uploaded Successfully",
            message = "Your iPhone 12 is now live and visible to buyers in your area",
            timestamp = "2 hours ago",
            type = NotificationType.ITEM_UPLOADED,
        ),
        Notification(
            id = "2",
            title = "New Item Near You",
            message = "Study Table available just 1.2 km away for ₹2,500",
            timestamp = "5 hours ago",
            type = NotificationType.NEW_ITEM_NEARBY,
        ),
        Notification(
            id = "3",
            title = "Item Uploaded Successfully",
            message = "Your Gaming Mouse is now available for sale",
            timestamp = "1 day ago",
            type = NotificationType.ITEM_UPLOADED,
        ),
        Notification(
            id = "4",
            title = "Welcome to Thrift It",
            message = "Start buying and selling items in your neighborhood",
            timestamp = "2 days ago",
            type = NotificationType.GENERAL,
        ),
    )
