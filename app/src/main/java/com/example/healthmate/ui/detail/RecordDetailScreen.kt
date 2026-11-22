package com.example.healthmate.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.ui.components.AppSnackbarHost
import com.example.healthmate.ui.components.AppTopBar
import com.example.healthmate.ui.components.UserMessage
import com.example.healthmate.ui.components.showMessage
import com.example.healthmate.ui.components.ConfirmDeleteDialog
import com.example.healthmate.ui.components.DestructiveButton
import com.example.healthmate.ui.components.EmptyState
import com.example.healthmate.ui.components.HealthStat
import com.example.healthmate.ui.components.PrimaryButton
import com.example.healthmate.ui.components.SecondaryButton
import com.example.healthmate.ui.components.StatCard
import com.example.healthmate.ui.components.formatRecordDate
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing
import java.io.File

/**
 * Record View screen, spec 4.5.
 *
 * This stateful version gets the ViewModel and reacts to one-off results; all drawing happens in
 * the stateless [RecordDetailContent]. Navigation is passed in as lambdas:
 * - [onDeleted] receives "Record deleted"; the caller goes back and shows it.
 */
@Composable
fun RecordDetailScreen(
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (recordId: Long) -> Unit,
    onDeleted: (message: String) -> Unit,
    viewModel: RecordDetailViewModel = viewModel(factory = RecordDetailViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.deletedMessage) {
        uiState.deletedMessage?.let(onDeleted)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showMessage(UserMessage(it, isError = true))
            viewModel.onErrorMessageShown()
        }
    }

    RecordDetailContent(
        state = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onAddNew = onAddNew,
        onEdit = onEdit,
        onDeleteClick = viewModel::onDeleteClick
    )

    if (uiState.showDeleteDialog) {
        ConfirmDeleteDialog(
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDismissDelete,
            recordDate = uiState.record?.date
        )
    }
}

@Composable
fun RecordDetailContent(
    state: RecordDetailUiState,
    onBack: () -> Unit,
    onAddNew: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "Record View", onBack = onBack) },
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        val record = state.record
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            record == null -> EmptyState(
                title = state.loadError ?: "This record no longer exists.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                actionLabel = "Go back",
                onAction = onBack
            )

            else -> RecordDetails(
                record = record,
                actionsEnabled = !state.isDeleting && state.deletedMessage == null,
                onAddNew = onAddNew,
                onEdit = { onEdit(record.id) },
                onDeleteClick = onDeleteClick,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun RecordDetails(
    record: HealthRecord,
    actionsEnabled: Boolean,
    onAddNew: () -> Unit,
    onEdit: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // "Wed, 19 Nov 2026"
        Text(
            text = formatRecordDate(record.date),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )

        record.photoPath?.let { path ->
            AsyncImage(
                model = File(path),
                contentDescription = "Selfie for ${formatRecordDate(record.date)}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }

        StatCard(HealthStat.STEPS, record.steps, Modifier.fillMaxWidth(), label = "Steps walked")
        StatCard(HealthStat.CALORIES, record.calories, Modifier.fillMaxWidth())
        StatCard(HealthStat.WATER, record.water, Modifier.fillMaxWidth())

        Spacer(Modifier.height(Spacing.xs))
        PrimaryButton(
            text = "Add New",
            onClick = onAddNew,
            icon = Icons.Filled.Add,
            enabled = actionsEnabled,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SecondaryButton(
                text = "Edit",
                onClick = onEdit,
                icon = Icons.Outlined.Edit,
                enabled = actionsEnabled,
                modifier = Modifier.weight(1f)
            )
            DestructiveButton(
                text = "Delete",
                onClick = onDeleteClick,
                icon = Icons.Outlined.Delete,
                enabled = actionsEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------- Previews ----------

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun RecordDetailPreview() {
    HealthMateTheme {
        RecordDetailContent(
            state = RecordDetailUiState(
                record = HealthRecord(id = 12, date = "2026-11-19", steps = 8450, calories = 320, water = 2100),
                isLoading = false
            ),
            onBack = {}, onAddNew = {}, onEdit = {}, onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordDetailMissingPreview() {
    HealthMateTheme {
        RecordDetailContent(
            state = RecordDetailUiState(isLoading = false, loadError = "This record no longer exists."),
            onBack = {}, onAddNew = {}, onEdit = {}, onDeleteClick = {}
        )
    }
}
