package com.example.thriftit.presentation.screens.buy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thriftit.data.repository.AuthRepository
import com.example.thriftit.data.repository.ItemRepository
import com.example.thriftit.data.repository.UserRepository
import com.example.thriftit.domain.models.Item
import com.example.thriftit.domain.models.ItemCategory
import com.example.thriftit.domain.util.Result
import com.example.thriftit.presentation.util.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuyViewModel
    @Inject
    constructor(
        private val itemRepository: ItemRepository,
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
    ) : ViewModel() {

        // Internal cache — not in UI state
        private val allItems = MutableStateFlow<List<Item>>(emptyList())

        // Single source of truth for the screen
        private val _uiState = MutableStateFlow(BuyUiState())
        val uiState: StateFlow<BuyUiState> = _uiState.asStateFlow()

        init {
            observeItems()
        }

        // ── Data ──────────────────────────────────────────────────────────────

        private fun observeItems() {
            viewModelScope.launch {
                itemRepository.observeItems().collect { items ->
                    allItems.value = attachDistance(items)
                    applyFilters()
                }
            }
        }

        fun refreshItems() {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true) }
                try { itemRepository.refreshOnce() }
                finally { _uiState.update { it.copy(isRefreshing = false) } }
            }
        }

        fun loadSellerPhone(sellerId: String) {
            viewModelScope.launch {
                userRepository.getUserById(sellerId).collect { result ->
                    if (result is Result.Success) {
                        _uiState.update { it.copy(sellerPhone = result.data?.phoneNumber) }
                    }
                }
            }
        }

        fun clearSellerPhone() = _uiState.update { it.copy(sellerPhone = null) }

        // ── Search ────────────────────────────────────────────────────────────

        fun updateSearchQuery(query: String) {
            _uiState.update { it.copy(searchQuery = query) }
            viewModelScope.launch {
                if (query.isBlank()) {
                    applyFilters()
                } else {
                    itemRepository.searchItems(query).collect { items ->
                        val withDistance = attachDistance(items)
                        _uiState.update { it.copy(items = UiState.Success(applyFiltersToList(withDistance))) }
                    }
                }
            }
        }

        // ── Filters ───────────────────────────────────────────────────────────

        fun updateCategoryFilter(category: ItemCategory?) {
            _uiState.update { it.copy(selectedCategory = category) }
            applyFilters()
        }

        fun updatePriceRange(min: Double, max: Double) {
            _uiState.update { it.copy(priceRange = PriceRange(min, max)) }
            applyFilters()
        }

        fun updateMaxDistance(distance: Double) {
            _uiState.update { it.copy(maxDistance = distance) }
            applyFilters()
        }

        fun updateSortOption(option: SortOption) {
            _uiState.update { it.copy(sortOption = option) }
            applyFilters()
        }

        fun clearFilters() {
            _uiState.update {
                it.copy(
                    selectedCategory = null,
                    priceRange = PriceRange(0.0, Double.MAX_VALUE),
                    maxDistance = null,
                    searchQuery = "",
                )
            }
            applyFilters()
        }

        // ── Filter logic ──────────────────────────────────────────────────────

        private fun applyFilters() {
            val filtered = applyFiltersToList(allItems.value)
            _uiState.update {
                it.copy(
                    items = if (filtered.isEmpty()) UiState.Error("No items match your filters")
                            else UiState.Success(filtered),
                )
            }
        }

        private fun applyFiltersToList(items: List<Item>): List<Item> {
            val state = _uiState.value
            var filtered = items

            state.selectedCategory?.let { filtered = filtered.filter { i -> i.category == it } }
            filtered = filtered.filter { it.price in state.priceRange.min..state.priceRange.max }
            state.maxDistance?.let { max -> filtered = filtered.filter { it.distance != null && it.distance <= max } }

            return when (state.sortOption) {
                SortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
                SortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
                SortOption.NEAREST -> filtered.sortedBy { it.distance ?: Double.MAX_VALUE }
            }
        }

        // ── Distance ──────────────────────────────────────────────────────────

        private suspend fun attachDistance(items: List<Item>): List<Item> {
            val userCoords = authRepository.getCurrentUserCoordinatesFromFirestore() ?: return items
            return items.map { item ->
                val coords = item.coordinates ?: return@map item
                item.copy(distance = haversine(userCoords.latitude, userCoords.longitude, coords.latitude, coords.longitude))
            }
        }

        private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = Math.sin(dLat / 2).let { it * it } +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).let { it * it }
            return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        }
    }

enum class SortOption { PRICE_LOW_TO_HIGH, PRICE_HIGH_TO_LOW, NEAREST }

data class PriceRange(val min: Double = 0.0, val max: Double = Double.MAX_VALUE)
