package com.example.healthmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing

/** Longest number any field accepts (100,000 has 6 digits). Range checks happen in validation. */
private const val MAX_DIGITS = 6

/**
 * Text field for whole numbers (steps, calories, water).
 * - Opens the number keyboard.
 * - Ignores anything that is not a digit, so "12a" or "-5" can't be typed.
 * - Shows [error] in red under the field when it is not null.
 */
@Composable
fun NumberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    leadingIcon: ImageVector? = null,
    error: String? = null,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val digits = input.filter { it.isDigit() }.take(MAX_DIGITS)
            onValueChange(digits)
        },
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        suffix = unit?.let { { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        shape = MaterialTheme.shapes.small
    )
}

@Preview(showBackground = true)
@Composable
private fun NumberInputFieldPreview() {
    HealthMateTheme {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            NumberInputField(
                value = "8450",
                onValueChange = {},
                label = "Steps walked",
                leadingIcon = HealthStat.STEPS.icon
            )
            NumberInputField(
                value = "12000",
                onValueChange = {},
                label = "Water intake",
                unit = "ml",
                leadingIcon = HealthStat.WATER.icon,
                error = "Enter water between 0 and 10,000 ml"
            )
        }
    }
}
