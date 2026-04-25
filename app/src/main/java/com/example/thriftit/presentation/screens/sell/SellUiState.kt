package com.example.thriftit.presentation.screens.sell

import android.net.Uri
import com.example.thriftit.domain.models.ItemCategory
import com.example.thriftit.domain.models.ItemCondition
import com.example.thriftit.presentation.util.UploadUiState

data class SellUiState(
    val title: String = "",
    val price: String = "",
    val description: String = "",
    val selectedImages: List<Uri> = emptyList(),
    val category: ItemCategory? = null,
    val condition: ItemCondition? = null,
    val uploadState: UploadUiState = UploadUiState.Idle,
    val validationErrors: Map<String, String> = emptyMap(),
)
