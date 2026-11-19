package com.example.healthmate.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.healthmate.ui.theme.BorderWidth
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing
import java.io.File

/**
 * Optional selfie for a record.
 *
 * - No photo: a tappable box that opens the Android Photo Picker (no storage permission needed).
 * - With a photo: a preview plus "Change" and "Remove" actions.
 *
 * [photo] is either a saved file path (String) or a just-picked [Uri]. This component only reports
 * the picked Uri through [onPhotoPicked]; copying it into the app's private storage is done by
 * the Add/Edit screen's ViewModel.
 */
@Composable
fun PhotoPickerBox(
    photo: Any?,
    onPhotoPicked: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) onPhotoPicked(uri) }

    val openPicker = {
        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val shape = MaterialTheme.shapes.medium

    Column(modifier.fillMaxWidth()) {
        if (photo == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline), shape)
                    .clickable(role = Role.Button, onClickLabel = "Add selfie", onClick = openPicker),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(Spacing.sm))
                Text("Add a selfie", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Optional",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            AsyncImage(
                model = if (photo is String) File(photo) else photo,
                contentDescription = "Selected selfie",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = openPicker) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.xs))
                    Text("Change")
                }
                TextButton(onClick = onRemovePhoto) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(Spacing.xs))
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoPickerBoxEmptyPreview() {
    HealthMateTheme {
        PhotoPickerBox(photo = null, onPhotoPicked = {}, onRemovePhoto = {}, modifier = Modifier.padding(Spacing.md))
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoPickerBoxWithPhotoPreview() {
    HealthMateTheme {
        // The image itself can't load in a preview; this shows the layout with Change / Remove.
        PhotoPickerBox(
            photo = "/preview/selfie.jpg",
            onPhotoPicked = {},
            onRemovePhoto = {},
            modifier = Modifier.padding(Spacing.md)
        )
    }
}
