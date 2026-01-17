package com.example.thriftit.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thriftit.core.network.NetworkObserver
import com.example.thriftit.core.network.NetworkStatus
import com.example.thriftit.data.local.dao.ItemDao
import com.example.thriftit.data.mappers.toEntity
import com.example.thriftit.data.repository.AuthRepository
import com.example.thriftit.data.repository.UploadRepository
import com.example.thriftit.data.repository.UserRepository
import com.example.thriftit.domain.models.Coordinates
import com.example.thriftit.domain.models.Item
import com.example.thriftit.domain.models.ItemCategory
import com.example.thriftit.domain.models.ItemCondition
import com.example.thriftit.domain.util.Result
import com.example.thriftit.presentation.util.UploadUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SellViewModel
    @Inject
    constructor(
        private val uploadRepository: UploadRepository,
        private val authRepository: AuthRepository,
        private val userRepository: UserRepository,
        private val itemDao: ItemDao,
        private val networkObserver: NetworkObserver,
    ) : ViewModel() {
        private val networkStatus =
            networkObserver.networkStatus
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = NetworkStatus.UNAVAILABLE,
                )

        // ---------------- UPLOAD STATE ----------------

        private val _uploadState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
        val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

        // ---------------- FORM STATE ----------------

        private val _title = MutableStateFlow("")
        val title: StateFlow<String> = _title.asStateFlow()

        private val _description = MutableStateFlow("")
        val description: StateFlow<String> = _description.asStateFlow()

        private val _price = MutableStateFlow("")
        val price: StateFlow<String> = _price.asStateFlow()

        private val _category = MutableStateFlow<ItemCategory?>(null)
        val category: StateFlow<ItemCategory?> = _category.asStateFlow()

        private val _condition = MutableStateFlow<ItemCondition?>(null)
        val condition: StateFlow<ItemCondition?> = _condition.asStateFlow()

        private val _location = MutableStateFlow("")
        val location: StateFlow<String> = _location.asStateFlow()

        private val _coordinates = MutableStateFlow<Coordinates?>(null)
        val coordinates: StateFlow<Coordinates?> = _coordinates.asStateFlow()

        // ---------------- IMAGE STATE ----------------

        private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
        val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

        // ---------------- VALIDATION ----------------

        private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
        val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    /* ============================================================
       IMAGE HANDLING
       ============================================================ */

        init {
            loadCurrentUserLocation()
        }

        private fun loadCurrentUserLocation() {
            val userId = authRepository.getCurrentUserId() ?: return

            viewModelScope.launch {
                userRepository.getUserById(userId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val user = result.data ?: return@collect

                            _location.value = user.location ?: ""
                            _coordinates.value = user.coordinates

                            // 🔥 DEBUG LOG (important)
                            android.util.Log.d(
                                "SELL_LOCATION",
                                "Loaded user location: ${user.coordinates}",
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }

        fun addImage(uri: Uri) {
            _selectedImages.value =
                if (uri !in _selectedImages.value) {
                    _selectedImages.value + uri
                } else {
                    _selectedImages.value
                }
        }

        fun removeImage(uri: Uri) {
            _selectedImages.value = _selectedImages.value - uri
        }

    /* ============================================================
       FORM UPDATERS (OPTIONAL BUT CLEAN)
       ============================================================ */

        fun updateTitle(value: String) {
            _title.value = value
        }

        fun updateDescription(value: String) {
            _description.value = value
        }

        fun updatePrice(value: String) {
            _price.value = value
        }

        fun updateCategory(value: ItemCategory?) {
            _category.value = value
        }

        fun updateCondition(value: ItemCondition?) {
            _condition.value = value
        }

        fun updateLocation(value: String) {
            _location.value = value
        }

        fun updateCoordinates(value: Coordinates?) {
            _coordinates.value = value
        }

    /* ============================================================
       UPLOAD LOGIC
       ============================================================ */

        fun uploadItem() {
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uploadState.value = UploadUiState.Error("User not authenticated")
                return
            }

            if (!validateForm()) {
                _uploadState.value = UploadUiState.Error("Please fix the errors")
                return
            }

            viewModelScope.launch {
                val item =
                    Item(
                        id = UUID.randomUUID().toString(),
                        title = _title.value,
                        description = _description.value,
                        price = _price.value.toDouble(),
                        category = _category.value ?: ItemCategory.OTHER,
                        condition = _condition.value!!,
                        imageUrls = emptyList(),
                        sellerId = userId,
                        location = _location.value,
                        coordinates = _coordinates.value,
                        isAvailable = true,
                    )

                // 🚨 OFFLINE → DO NOT SET Uploading
                if (networkStatus.value != NetworkStatus.AVAILABLE) {
                    itemDao.insertItem(
                        item.toEntity(
                            pendingUpload = true,
                            isSynced = false,
                            localImageUris = _selectedImages.value,
                        ),
                    )

                    _uploadState.value = UploadUiState.Success
                    resetForm()
                    return@launch
                }

                // 🌐 ONLINE → ONLY HERE Uploading is valid
                _uploadState.value = UploadUiState.Uploading(0f)

                uploadRepository
                    .uploadItemWithImages(item, _selectedImages.value)
                    .collect { result ->
                        _uploadState.value =
                            when (result) {
                                is Result.Loading -> UploadUiState.Uploading(50f)
                                is Result.Success -> UploadUiState.Success
                                is Result.Error -> UploadUiState.Error(result.message)
                            }
                    }
            }
        }

    /* ============================================================
       RESET
       ============================================================ */

        fun resetForm() {
            _title.value = ""
            _description.value = ""
            _price.value = ""
            _category.value = null
            _condition.value = null
            _location.value = ""
            _coordinates.value = null
            _selectedImages.value = emptyList()
            _validationErrors.value = emptyMap()
            _uploadState.value = UploadUiState.Idle
        }

        private fun validateForm(): Boolean {
            val errors = mutableMapOf<String, String>()

            if (_title.value.isBlank()) {
                errors["title"] = "Item name is required"
            }

            if (_price.value.isBlank()) {
                errors["price"] = "Price is required"
            } else if (_price.value.toDoubleOrNull() == null) {
                errors["price"] = "Invalid price"
            }

            if (_description.value.isBlank()) {
                errors["description"] = "Description is required"
            }

            if (_condition.value == null) {
                errors["condition"] = "Please select item condition"
            }

            if (_selectedImages.value.isEmpty()) {
                errors["images"] = "At least one image is required"
            }

            _validationErrors.value = errors
            return errors.isEmpty()
        }
    }
