package com.example.thriftit.presentation.screens.buy

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.thriftit.domain.models.Item
import com.example.thriftit.presentation.components.FilterBottomSheet
import com.example.thriftit.presentation.components.ItemDetail
import com.example.thriftit.presentation.components.ItemDetailDialog
import com.example.thriftit.presentation.util.UiState
import com.example.thriftit.presentation.viewmodel.BuyViewModel

@Composable
fun BuyScreen(viewModel: BuyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val hasActiveFilters = viewModel.hasActiveFilters

    var showFilters by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = pullToRefreshState,
            onRefresh = { viewModel.refreshItems() },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                SearchBar(
                    searchQuery = searchQuery,
                    hasActiveFilters = hasActiveFilters,
                    onSearchChange = viewModel::updateSearchQuery,
                    onFilterClick = { showFilters = true },
                )

                when (uiState) {
                    is UiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    is UiState.Success -> {
                        val items = (uiState as UiState.Success<List<Item>>).data
                        if (items.isEmpty()) {
                            EmptyState()
                        } else {
                            ItemGrid(
                                items = items,
                                viewModel = viewModel,
                                onItemClick = { item ->
                                    selectedItem = item
                                    viewModel.loadSellerPhone(item.sellerId)
                                },
                            )
                        }
                    }

                    is UiState.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = (uiState as UiState.Error).message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    UiState.Idle -> Unit
                }
            }
        }
    }

    if (showFilters) {
        FilterBottomSheet(
            initialPriceRange =
                viewModel.priceRange
                    .collectAsState()
                    .value.min
                    .toFloat()..viewModel.priceRange
                    .collectAsState()
                    .value.max
                    .toFloat(),
            initialMaxDistance = viewModel.maxDistance.value?.toFloat() ?: 10f,
            onDismiss = { showFilters = false },
            onClearAll = { viewModel.clearFilters() },
            onApplyFilters = { priceRange, distance ->
                viewModel.updatePriceRange(
                    priceRange.start.toDouble(),
                    priceRange.endInclusive.toDouble(),
                )
                viewModel.updateMaxDistance(distance.toDouble())
                showFilters = false
            },
        )
    }

    val context = LocalContext.current
    val sellerPhone by viewModel.sellerPhone.collectAsState()

    Log.d("CONNECT_DEBUG", "Seller phone = $sellerPhone")
    selectedItem?.let { item ->
        ItemDetailDialog(
            item = item.toItemDetail(),
            sellerPhone = sellerPhone,
            onDismiss = {
                selectedItem = null
                viewModel.clearSellerPhone()
            },
            onConnect = {
                openWhatsApp(context, "+918511010818", item.title)
            },
        )
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    hasActiveFilters: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "Search items…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                ),
        )

        Box {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            if (hasActiveFilters) {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-6).dp, y = 6.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(50),
                            ),
                )
            }
        }
    }
}

@Composable
private fun ItemGrid(
    items: List<Item>,
    onItemClick: (Item) -> Unit,
    viewModel: BuyViewModel,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items) { item ->
            ItemCard(
                item = item,
                onClick = {
                    viewModel.loadSellerPhone(item.sellerId)
                    onItemClick(item)
                },
            )
        }
    }
}

@Composable
private fun ItemCard(
    item: Item,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column {
            val context = LocalContext.current

            AsyncImage(
                model =
                    ImageRequest
                        .Builder(context)
                        .data(item.imageUrls.firstOrNull())
                        .crossfade(true)
                        .build(),
                contentDescription = item.title,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                contentScale = ContentScale.Crop,
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "₹${item.price.toInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ItemTag(item.condition.displayName)
                    ItemTag(item.distance?.let { "${it.format(1)} km" } ?: "Nearby")
                }
            }
        }
    }
}

@Composable
private fun ItemTag(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No items available",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun openWhatsApp(
    context: Context,
    phone: String,
    itemTitle: String,
) {
    val message = "Hi, I am interested in your item: $itemTitle"
    val url =
        "https://wa.me/${phone.replace("+", "")}?text=${Uri.encode(message)}"

    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
    }
}

private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

fun Item.toItemDetail() =
    ItemDetail(
        name = title,
        price = price,
        description = description,
        imageUrls = imageUrls.firstOrNull().orEmpty(),
        itemAge = condition.displayName,
        distance = distance ?: 0.0,
        sellerName = sellerName ?: "Seller",
        sellerPhone = "",
    )

@Preview(showBackground = true)
@Composable
private fun BuyScreenPreview() {
    MaterialTheme {
        BuyScreen()
    }
}
