package com.example.healthmate.ui.addedit

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthmate.domain.RecordFormErrors
import com.example.healthmate.domain.RecordValidation
import com.example.healthmate.ui.components.AppTopBar
import com.example.healthmate.ui.components.DatePickerField
import com.example.healthmate.ui.components.HealthStat
import com.example.healthmate.ui.components.NumberInputField
import com.example.healthmate.ui.components.PhotoPickerBox
import com.example.healthmate.ui.components.PrimaryButton
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing
import java.time.LocalDate

/**
 * Add New Record screen, spec 4.3 (written so Edit Record, spec 4.4, can reuse it).
 *
 * This stateful version gets the ViewModel and reacts to one-off results; all drawing happens in
 * the stateless [AddEditRecordContent]. Navigation is passed in as lambdas:
 * - [onSaved] receives "Record saved" / "Record updated"; the caller goes back and shows it.
 * - [onEditExisting] opens the record that already uses the chosen date.
 */
@Composable
fun AddEditRecordScreen(
    onBack: () -> Unit,
    onSaved: (message: String) -> Unit,
    onEditExisting: (recordId: Long) -> Unit,
    viewModel: AddEditRecordViewModel = viewModel(factory = AddEditRecordViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Saved successfully → hand the message to the caller, which navigates back.
    LaunchedEffect(uiState.savedMessage) {
        uiState.savedMessage?.let(onSaved)
    }

    // Database problem → show a friendly message on this screen, then mark it as shown.
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.onErrorMessageShown()
        }
    }

    AddEditRecordContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onDateSelected = viewModel::onDateSelected,
        onStepsChange = viewModel::onStepsChange,
        onCaloriesChange = viewModel::onCaloriesChange,
        onWaterChange = viewModel::onWaterChange,
        onPhotoPicked = viewModel::onPhotoPicked,
        onRemovePhoto = viewModel::onPhotoRemoved,
        onEditExisting = onEditExisting,
        onSave = viewModel::saveRecord
    )
}

@Composable
fun AddEditRecordContent(
    state: AddEditUiState,
    onBack: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onStepsChange: (String) -> Unit,
    onCaloriesChange: (String) -> Unit,
    onWaterChange: (String) -> Unit,
    onPhotoPicked: (Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    onEditExisting: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = state.title, onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // Keeps the focused field and the Save button above the on-screen keyboard.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            DatePickerField(
                value = state.date,
                onDateSelected = onDateSelected,
                label = "Date",
                maxDate = state.today, // future dates are greyed out in the calendar
                error = state.errors.date
            )
            if (state.duplicateRecordId != null) {
                // Spec 5: "A record for this date already exists" + option to edit that record.
                TextButton(onClick = { onEditExisting(state.duplicateRecordId) }) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(Spacing.xs))
                    Text("Edit the existing record")
                }
            }

            NumberInputField(
                value = state.steps,
                onValueChange = onStepsChange,
                label = "Steps walked",
                leadingIcon = HealthStat.STEPS.icon,
                error = state.errors.steps
            )
            NumberInputField(
                value = state.calories,
                onValueChange = onCaloriesChange,
                label = "Calories burned",
                unit = HealthStat.CALORIES.unit,
                leadingIcon = HealthStat.CALORIES.icon,
                error = state.errors.calories
            )
            NumberInputField(
                value = state.water,
                onValueChange = onWaterChange,
                label = "Water intake",
                unit = HealthStat.WATER.unit,
                leadingIcon = HealthStat.WATER.icon,
                error = state.errors.water,
                imeAction = ImeAction.Done // last field: the keyboard's button closes the keyboard
            )

            // Optional selfie: Photo Picker (no permission needed), preview, change or remove.
            Spacer(Modifier.height(Spacing.xs))
            Text("Selfie photo (optional)", style = MaterialTheme.typography.titleSmall)
            PhotoPickerBox(
                photo = state.photoPath,
                onPhotoPicked = onPhotoPicked,
                onRemovePhoto = onRemovePhoto
            )

            Spacer(Modifier.height(Spacing.sm))
            PrimaryButton(
                text = if (state.isSaving) "Saving…" else state.saveButtonLabel,
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------- Previews ----------

private val PreviewToday = LocalDate.of(2026, 11, 21)

@Preview(showBackground = true)
@Composable
private fun AddRecordEmptyPreview() {
    HealthMateTheme {
        AddEditRecordContent(
            state = AddEditUiState(today = PreviewToday, date = PreviewToday),
            onBack = {}, onDateSelected = {}, onStepsChange = {}, onCaloriesChange = {},
            onWaterChange = {}, onPhotoPicked = {}, onRemovePhoto = {}, onEditExisting = {}, onSave = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddRecordErrorsPreview() {
    HealthMateTheme {
        AddEditRecordContent(
            state = AddEditUiState(
                today = PreviewToday,
                date = PreviewToday,
                steps = "",
                calories = "12000",
                water = "2100",
                duplicateRecordId = 12,
                errors = RecordFormErrors(
                    date = RecordValidation.ERROR_DATE_TAKEN,
                    steps = RecordValidation.ERROR_STEPS,
                    calories = RecordValidation.ERROR_CALORIES
                )
            ),
            onBack = {}, onDateSelected = {}, onStepsChange = {}, onCaloriesChange = {},
            onWaterChange = {}, onPhotoPicked = {}, onRemovePhoto = {}, onEditExisting = {}, onSave = {}
        )
    }
}
