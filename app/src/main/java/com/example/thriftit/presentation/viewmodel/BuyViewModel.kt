package com.example.thriftit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thriftit.data.repository.ItemRepository
import com.example.thriftit.domain.models.Item
import com.example.thriftit.domain.models.ItemCategory
import com.example.thriftit.domain.util.Result
import com.example.thriftit.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuyViewModel
    @Inject
    constructor(
        private val itemRepository: ItemRepository,
    ) : ViewModel() {
        // Use generic UiState<List<Item>>
        private val _uiState = MutableStateFlow<UiState<List<Item>>>(UiState.Loading)
        val uiState: StateFlow<UiState<List<Item>>> = _uiState.asStateFlow()

        // Search query
        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        // Selected category filter
        private val _selectedCategory = MutableStateFlow<ItemCategory?>(null)
        val selectedCategory: StateFlow<ItemCategory?> = _selectedCategory.asStateFlow()

        // Price range filter
        private val _priceRange = MutableStateFlow(PriceRange(0.0, Double.MAX_VALUE))
        val priceRange: StateFlow<PriceRange> = _priceRange.asStateFlow()

        // Sort option
        private val _sortOption = MutableStateFlow(SortOption.NEAREST)
        val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

        private val allItems = MutableStateFlow<List<Item>>(emptyList())

        init {
            loadItems()
        }

        private fun loadItems() {
            viewModelScope.launch {
                itemRepository.getAllItems().collect { result ->
                    _uiState.value =
                        when (result) {
                            is Result.Loading -> UiState.Loading
                            is Result.Success -> {
                                allItems.value = result.data
                                if (result.data.isEmpty()) {
                                    UiState.Error("No items found")
                                } else {
                                    UiState.Success(result.data)
                                }
                            }
                            is Result.Error -> UiState.Error(result.message)
                        }
                }
            }
        }

        fun updateSearchQuery(query: String) {
            _searchQuery.value = query
            if (query.isNotEmpty()) {
                searchItems(query)
            } else {
                applyFilters()
            }
        }

        private fun searchItems(query: String) {
            viewModelScope.launch {
                _uiState.value = UiState.Loading
                itemRepository.searchItems(query).collect { result ->
                    _uiState.value =
                        when (result) {
                            is Result.Loading -> UiState.Loading
                            is Result.Success -> {
                                if (result.data.isEmpty()) {
                                    UiState.Error("No items found")
                                } else {
                                    val filtered = applyFiltersToList(result.data)
                                    UiState.Success(filtered)
                                }
                            }
                            is Result.Error -> UiState.Error(result.message)
                        }
                }
            }
        }

        fun updateCategoryFilter(category: ItemCategory?) {
            _selectedCategory.value = category
            applyFilters()
        }

        fun updatePriceRange(
            min: Double,
            max: Double,
        ) {
            _priceRange.value = PriceRange(min, max)
            applyFilters()
        }

        fun updateSortOption(option: SortOption) {
            _sortOption.value = option
            applyFilters()
        }

        private fun applyFilters() {
            val filtered = applyFiltersToList(allItems.value)
            _uiState.value =
                if (filtered.isEmpty()) {
                    UiState.Error("No items match your filters")
                } else {
                    UiState.Success(filtered)
                }
        }

        private fun applyFiltersToList(items: List<Item>): List<Item> {
            var filtered = items

            _selectedCategory.value?.let { category ->
                filtered = filtered.filter { it.category == category }
            }

            val range = _priceRange.value
            filtered = filtered.filter { it.price >= range.min && it.price <= range.max }

            filtered =
                when (_sortOption.value) {
                    SortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
                    SortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
                    SortOption.NEAREST -> filtered.sortedBy { it.distance ?: Double.MAX_VALUE }
                }

            return filtered
        }

        fun clearFilters() {
            _selectedCategory.value = null
            _priceRange.value = PriceRange(0.0, Double.MAX_VALUE)
            _searchQuery.value = ""
            applyFilters()
        }

        fun refreshItems() {
            loadItems()
        }
    }

enum class SortOption {
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    NEAREST,
}

data class PriceRange(
    val min: Double,
    val max: Double,
)
