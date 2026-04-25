package com.example.thriftit.presentation.screens.sell

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
import com.example.thriftit.data.util.ImageStorageHelper
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
import kotlinx.coroutines.flow.update
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
        private val imageStorageHelper: ImageStorageHelper,
    ) : ViewModel() {

        private val networkStatus =
            networkObserver.networkStatus
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetworkStatus.UNAVAILABLE)

        // Internal fields not shown in UI
        private var _location = ""
        private var _coordinates: Coordinates? = null

        // Single source of truth for the screen
        private val _uiState = MutableStateFlow(SellUiState())
        val uiState: StateFlow<SellUiState> = _uiState.asStateFlow()

        init {
            loadCurrentUserLocation()
        }

        // ── Location ──────────────────────────────────────────────────────────

        private fun loadCurrentUserLocation() {
            val userId = authRepository.getCurrentUserId() ?: return
            viewModelScope.launch {
                userRepository.getUserById(userId).collect { result ->
                    if (result is Result.Success) {
                        val user = result.data ?: return@collect
                        _location = user.location ?: ""
                        _coordinates = user.coordinates
                    }
                }
            }
        }

        // ── Form updates ─────────────────────────────────────────────────────

        fun updateTitle(value: String) = _uiState.update { it.copy(title = value) }
        fun updateDescription(value: String) = _uiState.update { it.copy(description = value) }
        fun updatePrice(value: String) = _uiState.update { it.copy(price = value) }
        fun updateCategory(value: ItemCategory?) = _uiState.update { it.copy(category = value) }
        fun updateCondition(value: ItemCondition?) = _uiState.update { it.copy(condition = value) }

        fun addImage(uri: Uri) = _uiState.update { state ->
            if (uri !in state.selectedImages) state.copy(selectedImages = state.selectedImages + uri)
            else state
        }

        fun removeImage(uri: Uri) = _uiState.update { it.copy(selectedImages = it.selectedImages - uri) }

        // ── Upload ───────────────────────────────────────────────────────────

        fun uploadItem() {
            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uiState.update { it.copy(uploadState = UploadUiState.Error("User not authenticated")) }
                return
            }
            if (!validateForm()) {
                _uiState.update { it.copy(uploadState = UploadUiState.Error("Please fix the errors")) }
                return
            }

            viewModelScope.launch {
                val state = _uiState.value
                val item = Item(
                    id = UUID.randomUUID().toString(),
                    title = state.title,
                    description = state.description,
                    price = state.price.toDouble(),
                    category = state.category ?: ItemCategory.OTHER,
                    condition = state.condition!!,
                    imageUrls = emptyList(),
                    sellerId = userId,
                    location = _location,
                    coordinates = _coordinates,
                    isAvailable = true,
                )

                if (networkStatus.value != NetworkStatus.AVAILABLE) {
                    val savedPaths = imageStorageHelper.saveImagesToInternalStorage(state.selectedImages)
                    if (savedPaths.size != state.selectedImages.size) {
                        _uiState.update { it.copy(uploadState = UploadUiState.Error("Failed to save some images")) }
                        return@launch
                    }
                    itemDao.insertItem(item.toEntity(pendingUpload = true, isSynced = false, localImagePaths = savedPaths))
                    _uiState.update { it.copy(uploadState = UploadUiState.Success) }
                    resetForm()
                    return@launch
                }

                _uiState.update { it.copy(uploadState = UploadUiState.Uploading(0f)) }
                uploadRepository.uploadItemWithImages(item, state.selectedImages).collect { result ->
                    _uiState.update {
                        it.copy(uploadState = when (result) {
                            is Result.Loading -> UploadUiState.Uploading(50f)
                            is Result.Success -> UploadUiState.Success
                            is Result.Error -> UploadUiState.Error(result.message)
                        })
                    }
                }
            }
        }

        // ── Reset ────────────────────────────────────────────────────────────

        fun resetForm() {
            _uiState.value = SellUiState()
        }

        // ── Validation ───────────────────────────────────────────────────────

        private fun validateForm(): Boolean {
            val state = _uiState.value
            val errors = mutableMapOf<String, String>()
            if (state.title.isBlank()) errors["title"] = "Item name is required"
            if (state.price.isBlank()) errors["price"] = "Price is required"
            else if (state.price.toDoubleOrNull() == null) errors["price"] = "Invalid price"
            if (state.description.isBlank()) errors["description"] = "Description is required"
            if (state.condition == null) errors["condition"] = "Please select item condition"
            if (state.category == null) errors["category"] = "Please select a category"
            if (state.selectedImages.isEmpty()) errors["images"] = "At least one image is required"
            _uiState.update { it.copy(validationErrors = errors) }
            return errors.isEmpty()
        }
    }
