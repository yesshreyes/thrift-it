package com.example.thriftit.presentation.screens.buy

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thriftit.domain.models.Item
import com.example.thriftit.presentation.components.FilterBottomSheet
import com.example.thriftit.presentation.components.ItemDetailDialog

@Composable
fun BuyScreen(viewModel: BuyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current

    var showFilters by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            state = pullToRefreshState,
            onRefresh = { viewModel.refreshItems() },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            },
        ) {
            BuyContent(
                uiState = uiState.items,
                searchQuery = uiState.searchQuery,
                hasActiveFilters = uiState.hasActiveFilters,
                onSearchChange = viewModel::updateSearchQuery,
                onFilterClick = { showFilters = true },
                onItemClick = { item ->
                    selectedItem = item
                    viewModel.loadSellerPhone(item.sellerId)
                },
            )
        }
    }

    if (showFilters) {
        FilterBottomSheet(
            initialPriceRange = uiState.priceRange.min.toFloat()..uiState.priceRange.max.toFloat(),
            initialMaxDistance = uiState.maxDistance?.toFloat() ?: 10f,
            onDismiss = { showFilters = false },
            onClearAll = { viewModel.clearFilters() },
            onApplyFilters = { range, distance ->
                viewModel.updatePriceRange(range.start.toDouble(), range.endInclusive.toDouble())
                viewModel.updateMaxDistance(distance.toDouble())
                showFilters = false
            },
        )
    }

    selectedItem?.let { item ->
        ItemDetailDialog(
            item = item.toItemDetail(),
            sellerPhone = uiState.sellerPhone,
            onDismiss = { selectedItem = null; viewModel.clearSellerPhone() },
            onConnect = { openWhatsApp(context, "+918511010818", item.title) },
        )
    }
}
