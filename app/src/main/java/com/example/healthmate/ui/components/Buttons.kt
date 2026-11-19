package com.example.healthmate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.healthmate.ui.theme.BorderWidth
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing

private val ButtonMinHeight = 48.dp
private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

/** Filled blue button for the main action on a screen: Add New Record, Save Record, Update Record. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        contentPadding = ButtonPadding
    ) {
        ButtonContent(text, icon)
    }
}

/** Outlined button for secondary actions: All Records, Cancel, Reset, Edit. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        contentPadding = ButtonPadding
    ) {
        ButtonContent(text, icon)
    }
}

/** Red outlined button for actions that remove data: Delete. */
@Composable
fun DestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ButtonMinHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.error),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        contentPadding = ButtonPadding
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
}

@Preview(showBackground = true)
@Composable
private fun ButtonsPreview() {
    HealthMateTheme {
        Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            PrimaryButton("Add New Record", onClick = {}, icon = Icons.Filled.Add, modifier = Modifier.fillMaxWidth())
            PrimaryButton("Save Record", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            SecondaryButton("All Records", onClick = {}, modifier = Modifier.fillMaxWidth())
            SecondaryButton("Edit", onClick = {}, icon = Icons.Outlined.Edit)
            DestructiveButton("Delete", onClick = {}, icon = Icons.Outlined.Delete)
        }
    }
}
