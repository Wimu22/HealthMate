package com.example.healthmate.ui.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthmate.data.local.HealthRecord
import com.example.healthmate.ui.components.AppTopBar
import com.example.healthmate.ui.components.ConfirmDeleteDialog
import com.example.healthmate.ui.components.EmptyState
import com.example.healthmate.ui.components.RecordCard
import com.example.healthmate.ui.theme.HealthMateTheme
import com.example.healthmate.ui.theme.Spacing

/**
 * All Records screen, spec 4.2.
 *
 * This stateful version gets the ViewModel, passes on Snackbar messages and shows the delete dialog;
 * all list drawing happens in the stateless [AllRecordsContent]. Navigation is passed in as lambdas.
 */
@Composable
fun AllRecordsScreen(
    onBack: () -> Unit,
    onAddRecord: () -> Unit,
    onOpenRecord: (recordId: Long) -> Unit,
    onEditRecord: (recordId: Long) -> Unit,
    onShowMessage: (message: String) -> Unit,
    viewModel: AllRecordsViewModel = viewModel(factory = AllRecordsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // "Record deleted" (or a friendly error): hand it to the app-wide Snackbar once, then clear it.
    // The app-wide Snackbar keeps showing even if the user leaves this screen straight away.
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            onShowMessage(it)
            viewModel.onMessageShown()
        }
    }

    AllRecordsContent(
        state = uiState,
        onBack = onBack,
        onAddRecord = onAddRecord,
        onOpenRecord = onOpenRecord,
        onEditRecord = onEditRecord,
        onDeleteRecord = viewModel::onDeleteClick
    )

    uiState.recordToDelete?.let { record ->
        ConfirmDeleteDialog(
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDismissDelete,
            recordDate = record.date
        )
    }
}

@Composable
fun AllRecordsContent(
    state: AllRecordsUiState,
    onBack: () -> Unit,
    onAddRecord: () -> Unit,
    onOpenRecord: (Long) -> Unit,
    onEditRecord: (Long) -> Unit,
    onDeleteRecord: (HealthRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppTopBar(title = "All Records", onBack = onBack) }
    ) { innerPadding ->
        val contentModifier = Modifier.fillMaxSize().padding(innerPadding)
        when {
            state.isLoading -> Box(contentModifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            state.loadError != null -> EmptyState(
                title = state.loadError,
                modifier = contentModifier,
                actionLabel = "Go back",
                onAction = onBack
            )

            state.records.isEmpty() -> EmptyState(
                title = "No records yet",
                message = "Start tracking your steps, calories and water.",
                modifier = contentModifier,
                actionLabel = "Add New Record",
                actionIcon = Icons.Filled.Add,
                onAction = onAddRecord
            )

            else -> LazyColumn(
                modifier = contentModifier,
                contentPadding = PaddingValues(horizontal = Spacing.screenHorizontal, vertical = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // key = id: Compose keeps each card's identity, so deleting one doesn't redraw the rest.
                items(state.records, key = { it.id }) { record ->
                    RecordCard(
                        record = record,
                        onClick = { onOpenRecord(record.id) },
                        onEdit = { onEditRecord(record.id) },
                        onDelete = { onDeleteRecord(record) } // ignored by the ViewModel while a delete runs
                    )
                }
            }
        }
    }
}

// ---------- Previews ----------

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun AllRecordsPreview() {
    HealthMateTheme {
        AllRecordsContent(
            state = AllRecordsUiState(
                records = listOf(
                    HealthRecord(id = 3, date = "2026-11-21", steps = 9_840, calories = 455, water = 2_350),
                    HealthRecord(id = 2, date = "2026-11-20", steps = 12_310, calories = 610, water = 2_900),
                    HealthRecord(id = 1, date = "2026-11-19", steps = 6_120, calories = 280, water = 1_800)
                ),
                isLoading = false
            ),
            onBack = {}, onAddRecord = {}, onOpenRecord = {}, onEditRecord = {}, onDeleteRecord = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
private fun AllRecordsEmptyPreview() {
    HealthMateTheme {
        AllRecordsContent(
            state = AllRecordsUiState(isLoading = false),
            onBack = {}, onAddRecord = {}, onOpenRecord = {}, onEditRecord = {}, onDeleteRecord = {}
        )
    }
}
