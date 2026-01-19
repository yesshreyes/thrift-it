package com.example.thriftit.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

data class ItemDetail(
    val name: String,
    val price: Double,
    val description: String,
    val imageUrls: String,
    val itemAge: String,
    val distance: Double,
    val sellerName: String,
    val sellerPhone: String,
)

@Composable
fun ItemDetailDialog(
    item: ItemDetail,
    sellerPhone: String?,
    onDismiss: () -> Unit = {},
    onConnect: () -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                // Close button
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Image
                AsyncImage(
                    model = item.imageUrls,
                    contentDescription = item.name,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                ) {
                    // Title
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Price
                    Text(
                        text = "₹${item.price.toInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description header
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Description
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Details
                    DetailRow(label = "Condition", value = item.itemAge)
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(
                        label = "Distance",
                        value = "${formatDistance(item.distance)} km away",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(label = "Seller", value = item.sellerName)

                    Spacer(modifier = Modifier.height(24.dp))

                    // CTA
                    Button(
                        onClick = onConnect,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "Connect with Seller",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatDistance(distance: Double): String = "%.1f".format(distance)

@Preview(showBackground = true)
@Composable
private fun ItemDetailDialogPreview() {
    MaterialTheme {
        ItemDetailDialog(
            item =
                ItemDetail(
                    name = "iPhone 12",
                    price = 35000.0,
                    description = "Excellent condition",
                    imageUrls = "https://via.placeholder.com/300",
                    itemAge = "1 year old",
                    distance = 2.5,
                    sellerName = "John Doe",
                    sellerPhone = "+919876543210",
                ),
            sellerPhone = "",
        )
    }
}
