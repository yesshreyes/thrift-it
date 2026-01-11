package com.example.thriftit.presentation.screens.sell

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.thriftit.domain.models.ItemCondition
import com.example.thriftit.presentation.util.UploadUiState
import com.example.thriftit.presentation.util.createImageUri
import com.example.thriftit.presentation.viewmodel.SellViewModel

private const val NAME_MAX_LENGTH = 50
private const val DESC_MAX_LENGTH = 300
private const val PRICE_MAX_LENGTH = 8

@Composable
fun SellScreen(viewModel: SellViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val selectedImages by viewModel.selectedImages.collectAsStateWithLifecycle()

    var itemName by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val selectedCondition by viewModel.condition.collectAsStateWithLifecycle()
    val validationErrors by viewModel.validationErrors.collectAsStateWithLifecycle()

    // ---------------- CAMERA PERMISSION ----------------

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (!granted) {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // ---------------- GALLERY PICKER ----------------

    val galleryPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
        ) { uri ->
            uri?.let { viewModel.addImage(it) }
        }

    // ---------------- CAMERA PICKER ----------------

    val cameraUri = remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture(),
        ) { success ->
            if (success) {
                cameraUri.value?.let { viewModel.addImage(it) }
            }
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { granted ->
            if (!granted) {
                Toast
                    .makeText(
                        context,
                        "Notifications disabled. You won't get upload alerts.",
                        Toast.LENGTH_SHORT,
                    ).show()
            }
        }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
    }

    val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()

    LaunchedEffect(uploadState) {
        when (uploadState) {
            is UploadUiState.Success -> {
                Toast.makeText(context, "Item uploaded successfully", Toast.LENGTH_SHORT).show()
                showUploadSuccessNotification(context)
                viewModel.resetForm()
            }

            is UploadUiState.Error -> {
                Toast
                    .makeText(
                        context,
                        (uploadState as UploadUiState.Error).message,
                        Toast.LENGTH_LONG,
                    ).show()
            }

            else -> Unit
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
        ) {
            SellHeader()

            Spacer(Modifier.height(24.dp))

            // ---------------- IMAGE PICKER ----------------

            Text(
                text = "Item Photos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        galleryPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) {
                    Text("Gallery")
                }

                Button(
                    onClick = {
                        if (
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA,
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            val uri = createImageUri(context)
                            cameraUri.value = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                ) {
                    Text("Camera")
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---------------- IMAGE PREVIEW ----------------

            if (selectedImages.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImages) { uri ->
                        Box {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop,
                            )

                            IconButton(
                                onClick = { viewModel.removeImage(uri) },
                                modifier =
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.padding(4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp),
                            ).clickable {
                                galleryPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Tap to add images")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ---------------- FORM ----------------

            SellForm(
                itemName = itemName,
                price = price,
                description = description,
                selectedCondition = selectedCondition,
                conditionError = validationErrors["condition"],
                onItemNameChange = {
                    itemName = it.take(NAME_MAX_LENGTH)
                    viewModel.updateTitle(itemName)
                },
                onPriceChange = {
                    price = it.filter(Char::isDigit).take(PRICE_MAX_LENGTH)
                    viewModel.updatePrice(price)
                },
                onDescriptionChange = {
                    description = it.take(DESC_MAX_LENGTH)
                    viewModel.updateDescription(description)
                },
                onConditionChange = viewModel::updateCondition,
            )

            Spacer(Modifier.height(32.dp))

            val uploadState by viewModel.uploadState.collectAsStateWithLifecycle()

            UploadButton(
                isEnabled =
                    itemName.isNotBlank() &&
                        price.isNotBlank() &&
                        description.isNotBlank() &&
                        selectedImages.isNotEmpty() &&
                        uploadState !is UploadUiState.Uploading,
                isLoading = uploadState is UploadUiState.Uploading,
                onClick = {
                    viewModel.uploadItem()
                },
            )

            Spacer(Modifier.height(16.dp))

            InfoNote()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemConditionDropdown(
    selectedCondition: ItemCondition?,
    error: String?,
    onSelect: (ItemCondition) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedCondition?.displayName ?: "",
            onValueChange = {},
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
            readOnly = true,
            isError = error != null,
            label = { Text("Item Condition") },
            placeholder = { Text("Select condition") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ItemCondition.entries.forEach { condition ->
                DropdownMenuItem(
                    text = { Text(condition.displayName) },
                    onClick = {
                        onSelect(condition)
                        expanded = false
                    },
                )
            }
        }
    }

    if (error != null) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun SellHeader() {
    Text(
        text = "Sell an Item",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Add details about the item you want to sell",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ImagePickerSection(
    selectedImageUri: Uri?,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Item Photo",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedImageUri != null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected item image",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )

                IconButton(
                    onClick = onRemoveImage,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
            }
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp),
                        ).clickable(onClick = onPickImage),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add photo",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to add photo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SellForm(
    itemName: String,
    price: String,
    description: String,
    selectedCondition: ItemCondition?,
    conditionError: String?,
    onItemNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConditionChange: (ItemCondition) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Item Name
        OutlinedTextField(
            value = itemName,
            onValueChange = onItemNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Item Name") },
            placeholder = { Text("iPhone 12, Study Table, etc.") },
            singleLine = true,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
        )

        // Price
        OutlinedTextField(
            value = price,
            onValueChange = onPriceChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Price (₹)") },
            placeholder = { Text("5000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
        )

        // Description
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") },
            placeholder = { Text("Describe the condition, features, etc.") },
            minLines = 3,
            maxLines = 5,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            supportingText = {
                Text(
                    text = "${description.length}/$DESC_MAX_LENGTH",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
        )

        ItemConditionDropdown(
            selectedCondition = selectedCondition,
            error = conditionError,
            onSelect = onConditionChange,
        )
    }
}

@Composable
private fun UploadButton(
    isEnabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
        enabled = isEnabled,
    ) {
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(12.dp))
                Text("Uploading…")
            }
        } else {
            Text(
                text = "Upload Item",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun showUploadSuccessNotification(context: Context) {
    val intent =
        context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

    val pendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    val notification =
        NotificationCompat
            .Builder(context, "upload_channel")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("Item Uploaded 🎉")
            .setContentText("Your item is now visible to nearby buyers")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

    NotificationManagerCompat
        .from(context)
        .notify(System.currentTimeMillis().toInt(), notification)
}

@Composable
private fun InfoNote() {
    Text(
        text = "Your item will be visible to buyers in your area once uploaded",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SellScreenPreview() {
    MaterialTheme {
        SellScreen()
    }
}
