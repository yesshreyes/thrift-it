package com.example.thriftit.ui.screens.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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

@Composable
fun NotificationScreen() {
    val notifications = getSampleNotifications() // TODO: Replace with ViewModel
    val isLoading = false // TODO: Replace with ViewModel state

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            NotificationHeader()

            if (isLoading) {
                LoadingState()
            } else if (notifications.isEmpty()) {
                EmptyState()
            } else {
                NotificationList(notifications = notifications)
            }
        }
    }
}

@Composable
private fun NotificationHeader() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
    ) {
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Stay updated with your listings",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotificationList(notifications: List<Notification>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(notifications) { notification ->
            NotificationCard(notification = notification)
        }
    }
}

@Composable
private fun NotificationCard(notification: Notification) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Notification Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color =
                    when (notification.type) {
                        NotificationType.ITEM_UPLOADED -> MaterialTheme.colorScheme.primaryContainer
                        NotificationType.NEW_ITEM_NEARBY -> MaterialTheme.colorScheme.tertiaryContainer
                        NotificationType.GENERAL -> MaterialTheme.colorScheme.secondaryContainer
                    },
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector =
                            when (notification.type) {
                                NotificationType.ITEM_UPLOADED -> Icons.Default.CheckCircle
                                NotificationType.NEW_ITEM_NEARBY -> Icons.Default.Notifications
                                NotificationType.GENERAL -> Icons.Default.Notifications
                            },
                        contentDescription = null,
                        tint =
                            when (notification.type) {
                                NotificationType.ITEM_UPLOADED -> MaterialTheme.colorScheme.onPrimaryContainer
                                NotificationType.NEW_ITEM_NEARBY -> MaterialTheme.colorScheme.onTertiaryContainer
                                NotificationType.GENERAL -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // Notification Content
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = notification.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No notifications yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "We'll notify you when something important happens",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// Sample data for preview
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun NotificationScreenPreview() {
    MaterialTheme {
        NotificationScreen()
    }
}