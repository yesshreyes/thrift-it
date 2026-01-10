package com.example.thriftit.presentation.viewmodel

import android.util.Log
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
        // -------------------- FILTER STATE --------------------

        private val _selectedCategory = MutableStateFlow<ItemCategory?>(null)
        val selectedCategory: StateFlow<ItemCategory?> = _selectedCategory.asStateFlow()

        private val _priceRange = MutableStateFlow(PriceRange(0.0, Double.MAX_VALUE))
        val priceRange: StateFlow<PriceRange> = _priceRange.asStateFlow()

        private val _maxDistance = MutableStateFlow<Double?>(null)
        val maxDistance: StateFlow<Double?> = _maxDistance.asStateFlow()

        private val _sortOption = MutableStateFlow(SortOption.NEAREST)
        val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

        private val _sellerPhone = MutableStateFlow<String?>(null)
        val sellerPhone: StateFlow<String?> = _sellerPhone.asStateFlow()

        private val _isFetchingSellerPhone = MutableStateFlow(false)
        val isFetchingSellerPhone: StateFlow<Boolean> = _isFetchingSellerPhone.asStateFlow()

        val hasActiveFilters: Boolean
            get() =
                _selectedCategory.value != null ||
                    _priceRange.value.min > 0.0 ||
                    _priceRange.value.max < Double.MAX_VALUE ||
                    _maxDistance.value != null

        // -------------------- UI STATE --------------------

        private val _uiState = MutableStateFlow<UiState<List<Item>>>(UiState.Loading)
        val uiState: StateFlow<UiState<List<Item>>> = _uiState.asStateFlow()

        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        private val allItems = MutableStateFlow<List<Item>>(emptyList())

        init {
            loadItems()
        }

        // -------------------- DATA LOADING --------------------

        fun loadSellerPhone(sellerId: String) {
            viewModelScope.launch {
                userRepository.getUserById(sellerId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val phone = result.data?.phoneNumber
                            Log.d("CONNECT_DEBUG", "Fetched phone = $phone")
                            _sellerPhone.value = phone
                        }
                        is Result.Error -> {
                            Log.e("CONNECT_DEBUG", "Failed to fetch seller", result.exception)
                        }
                        else -> Unit
                    }
                }
            }
        }

        fun clearSellerPhone() {
            _sellerPhone.value = null
        }

        private fun loadItems() {
            viewModelScope.launch {
                itemRepository.getAllItems().collect { result ->
                    _uiState.value =
                        when (result) {
                            is Result.Loading -> UiState.Loading
                            is Result.Success -> {
                                val withDistance = attachDistance(result.data)
                                allItems.value = withDistance
                                UiState.Success(applyFiltersToList(withDistance))
                            }
                            is Result.Error -> UiState.Error(result.message)
                        }
                }
            }
        }

        fun refreshItems() {
            loadItems()
        }

        // -------------------- SEARCH --------------------

        fun updateSearchQuery(query: String) {
            _searchQuery.value = query
            if (query.isBlank()) {
                applyFilters()
            } else {
                searchItems(query)
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
                                val withDistance = attachDistance(result.data)
                                UiState.Success(applyFiltersToList(withDistance))
                            }
                            is Result.Error -> UiState.Error(result.message)
                        }
                }
            }
        }

        // -------------------- FILTER UPDATES --------------------

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

        fun updateMaxDistance(distance: Double) {
            _maxDistance.value = distance
            applyFilters()
        }

        fun updateSortOption(option: SortOption) {
            _sortOption.value = option
            applyFilters()
        }

        fun clearFilters() {
            _selectedCategory.value = null
            _priceRange.value = PriceRange(0.0, Double.MAX_VALUE)
            _maxDistance.value = null
            _searchQuery.value = ""
            applyFilters()
        }

        // -------------------- FILTER LOGIC --------------------

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
            filtered = filtered.filter { it.price in range.min..range.max }

            _maxDistance.value?.let { max ->
                filtered = filtered.filter { it.distance != null && it.distance <= max }
            }

            return when (_sortOption.value) {
                SortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
                SortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
                SortOption.NEAREST -> filtered.sortedBy { it.distance ?: Double.MAX_VALUE }
            }
        }

        // -------------------- DISTANCE --------------------

        private suspend fun attachDistance(items: List<Item>): List<Item> {
            val userCoords =
                authRepository.getCurrentUserCoordinatesFromFirestore() ?: return items

            return items.map { item ->
                val coords = item.coordinates
                if (coords != null) {
                    val distance =
                        haversine(
                            userCoords.latitude,
                            userCoords.longitude,
                            coords.latitude,
                            coords.longitude,
                        )
                    item.copy(distance = distance)
                } else {
                    item
                }
            }
        }

        private fun haversine(
            lat1: Double,
            lon1: Double,
            lat2: Double,
            lon2: Double,
        ): Double {
            val r = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) *
                    Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)

            return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
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
