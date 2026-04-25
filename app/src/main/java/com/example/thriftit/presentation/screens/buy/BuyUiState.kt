package com.example.thriftit.presentation.screens.buy

import com.example.thriftit.domain.models.Item
import com.example.thriftit.domain.models.ItemCategory
import com.example.thriftit.presentation.util.UiState

data class BuyUiState(
    val items: UiState<List<Item>> = UiState.Loading,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val sellerPhone: String? = null,
    // Filter state
    val selectedCategory: ItemCategory? = null,
    val priceRange: PriceRange = PriceRange(),
    val maxDistance: Double? = null,
    val sortOption: SortOption = SortOption.NEAREST,
) {
    val hasActiveFilters: Boolean
        get() = selectedCategory != null ||
            priceRange.min > 0.0 ||
            priceRange.max < Double.MAX_VALUE ||
            maxDistance != null
}
