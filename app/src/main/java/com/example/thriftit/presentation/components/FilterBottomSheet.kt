package com.example.thriftit.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    initialPriceRange: ClosedFloatingPointRange<Float>,
    initialMaxDistance: Float,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onApplyFilters: (ClosedFloatingPointRange<Float>, Float) -> Unit,
) {
    var priceRange by remember { mutableStateOf(initialPriceRange) }
    var maxDistance by remember { mutableFloatStateOf(initialMaxDistance) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        FilterContent(
            priceRange = priceRange,
            maxDistance = maxDistance,
            onPriceRangeChange = { priceRange = it },
            onDistanceChange = { maxDistance = it },
            onClearAll = {
                priceRange = 0f..100000f
                maxDistance = 10f
                onClearAll()
                onDismiss()
            },
            onApplyFilters = {
                onApplyFilters(priceRange, maxDistance)
                onDismiss()
            },
        )
    }
}

@Composable
private fun FilterContent(
    priceRange: ClosedFloatingPointRange<Float>,
    maxDistance: Float,
    onPriceRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onDistanceChange: (Float) -> Unit,
    onClearAll: () -> Unit,
    onApplyFilters: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
    ) {
        // Header
        Text(
            text = "Filters",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Price Range
        Text(
            text = "Price Range",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "₹${priceRange.start.roundToInt()} - ₹${priceRange.endInclusive.roundToInt()}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        RangeSlider(
            value = priceRange,
            onValueChange = onPriceRangeChange,
            valueRange = 0f..100000f,
            steps = 99,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "₹0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "₹1,00,000",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Distance
        Text(
            text = "Maximum Distance",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${maxDistance.roundToInt()} km away",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Slider(
            value = maxDistance,
            onValueChange = onDistanceChange,
            valueRange = 1f..50f,
            steps = 48,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "1 km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "50 km",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Clear All",
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Button(
                onClick = onApplyFilters,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Apply Filters",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun FilterContentPreview() {
    MaterialTheme {
        Surface {
            FilterContent(
                priceRange = 5000f..50000f,
                maxDistance = 15f,
                onPriceRangeChange = {},
                onDistanceChange = {},
                onClearAll = {},
                onApplyFilters = {},
            )
        }
    }
}
