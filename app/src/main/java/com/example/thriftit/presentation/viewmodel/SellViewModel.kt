package com.example.thriftit.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.thriftit.data.repository.AuthRepository
import com.example.thriftit.data.repository.UploadRepository
import com.example.thriftit.domain.models.Coordinates
import com.example.thriftit.domain.models.Item
import com.example.thriftit.domain.models.ItemCategory
import com.example.thriftit.domain.models.ItemCondition
import com.example.thriftit.domain.util.Result
import com.example.thriftit.presentation.util.UploadUiState // Use your existing one
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellViewModel
    @Inject
    constructor(
        private val uploadRepository: UploadRepository,
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        // Use your existing UploadUiState
        private val _uploadState = MutableStateFlow<UploadUiState>(UploadUiState.Idle)
        val uploadState: StateFlow<UploadUiState> = _uploadState.asStateFlow()

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

        private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
        val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

        private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
        val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

        // ... all the update functions remain the same

        fun uploadItem() {
//        if (!validateForm()) {
//            _uploadState.value = UploadUiState.Error("Please fix validation errors")
//            return
//        }

            val userId = authRepository.getCurrentUserId()
            if (userId == null) {
                _uploadState.value = UploadUiState.Error("User not authenticated")
                return
            }

            viewModelScope.launch {
                _uploadState.value = UploadUiState.Uploading(0f)

                val item =
                    Item(
                        id = "",
                        title = _title.value,
                        description = _description.value,
                        price = _price.value.toDouble(),
                        category = _category.value!!,
                        condition = _condition.value!!,
                        imageUrls = emptyList(),
                        sellerId = userId,
                        location = _location.value,
                        coordinates = _coordinates.value,
                        isAvailable = true,
                    )

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
    }
