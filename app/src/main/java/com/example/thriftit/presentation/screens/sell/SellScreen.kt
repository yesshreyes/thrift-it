package com.example.thriftit.presentation.screens.sell

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.thriftit.presentation.util.UploadUiState
import com.example.thriftit.presentation.util.createImageUri

private const val NAME_MAX_LENGTH = 50
private const val DESC_MAX_LENGTH = 300
private const val PRICE_MAX_LENGTH = 8

@Composable
fun SellScreen(viewModel: SellViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.addImage(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
    }

    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri.value?.let { viewModel.addImage(it) }
    }

    LaunchedEffect(uiState.uploadState) {
        when (val state = uiState.uploadState) {
            is UploadUiState.Success -> {
                Toast.makeText(context, "Item uploaded successfully", Toast.LENGTH_SHORT).show()
                viewModel.resetForm()
            }
            is UploadUiState.Error -> Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            else -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        SellContent(
            uiState = uiState,
            onGalleryClick = { galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onCameraClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    val uri = createImageUri(context)
                    cameraUri.value = uri
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onRemoveImage = viewModel::removeImage,
            onItemNameChange = { viewModel.updateTitle(it.take(NAME_MAX_LENGTH)) },
            onPriceChange = { viewModel.updatePrice(it.filter(Char::isDigit).take(PRICE_MAX_LENGTH)) },
            onDescriptionChange = { viewModel.updateDescription(it.take(DESC_MAX_LENGTH)) },
            onCategoryChange = viewModel::updateCategory,
            onConditionChange = viewModel::updateCondition,
            onUploadClick = viewModel::uploadItem,
        )
    }
}
