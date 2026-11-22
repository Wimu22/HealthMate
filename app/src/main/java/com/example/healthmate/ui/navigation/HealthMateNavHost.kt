package com.example.healthmate.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthmate.domain.SortOrder
import com.example.healthmate.ui.addedit.AddEditRecordScreen
import com.example.healthmate.ui.components.AppSnackbarHost
import com.example.healthmate.ui.components.UserMessage
import com.example.healthmate.ui.components.showMessage
import com.example.healthmate.ui.dashboard.DashboardScreen
import com.example.healthmate.ui.detail.RecordDetailScreen
import com.example.healthmate.ui.records.AllRecordsScreen
import com.example.healthmate.ui.search.SearchResultsScreen
import kotlinx.coroutines.launch

/**
 * The navigation graph (spec section 6):
 *
 * ```
 * Dashboard ──► Add New Record
 *     │──────► All Records ──► Record View ──► Edit Record
 *     │──────► Record View (from Recent Records)
 *     └──────► Filter sheet ──► Search Results ──► Record View
 * ```
 *
 * Screens never get the NavController. They receive plain lambdas such as `onOpenRecord(id)`,
 * and only this file decides where each one goes. That keeps screens simple and testable.
 *
 * Back always returns to the previous screen (the system back button and the top bar arrow both
 * pop the back stack). Save, Update and Delete also pop back, as the spec requires.
 *
 * Success messages ("Record saved", …) use one Snackbar shared by the whole app, drawn on top of
 * the NavHost. That way the message stays visible on the previous screen after going back.
 */
@Composable
fun HealthMateNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val goBack: () -> Unit = { navController.popBackStack() }
    val openRecord: (Long) -> Unit = { id -> navController.navigate(Screen.RecordView.createRoute(id)) }
    val editRecord: (Long) -> Unit = { id -> navController.navigate(Screen.EditRecord.createRoute(id)) }
    val addRecord: () -> Unit = { navController.navigate(Screen.AddRecord.route) }

    val snackbarHostState = remember { SnackbarHostState() }
    // This scope lives as long as the NavHost, so the message survives leaving the Add screen.
    val scope = rememberCoroutineScope()
    val showMessage: (UserMessage) -> Unit = { message -> scope.launch { snackbarHostState.showMessage(message) } }

    /**
     * After Save, Update or Delete: go back and show the message ("Record saved", …). Only if
     * [entry] is still the top screen, so a repeated call can't pop the screen underneath too.
     */
    fun goBackWithMessage(entry: NavBackStackEntry): (String) -> Unit = { message ->
        if (navController.currentBackStackEntry == entry) {
            navController.popBackStack()
            showMessage(UserMessage(message)) // a success: saved, updated or deleted
        }
    }

    /** "Edit the existing record": replace this form with that record's Edit screen, so Back
     *  from it returns to where the form was opened from. */
    val replaceWithEdit: (Long) -> Unit = { id ->
        navController.popBackStack()
        editRecord(id)
    }

    Box(modifier) {
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddRecord = addRecord,
                    onAllRecords = { navController.navigate(Screen.AllRecords.route) },
                    onOpenRecord = openRecord,
                    onSearch = { startDate, endDate, sortOrder ->
                        navController.navigate(Screen.SearchResults.createRoute(startDate, endDate, sortOrder))
                    }
                )
            }

            composable(Screen.AllRecords.route) {
                AllRecordsScreen(
                    onBack = goBack,
                    onAddRecord = addRecord,
                    onOpenRecord = openRecord,
                    onEditRecord = editRecord,
                    // Delete happens on this screen (we stay here), so just show "Record deleted".
                    onShowMessage = showMessage
                )
            }

            composable(Screen.AddRecord.route) { entry ->
                AddEditRecordScreen(
                    onBack = goBack,
                    onSaved = goBackWithMessage(entry),
                    onEditExisting = replaceWithEdit
                )
            }

            composable(
                route = Screen.EditRecord.route,
                arguments = listOf(navArgument(Screen.ARG_RECORD_ID) { type = NavType.LongType })
            ) { entry ->
                // Same screen as Add; the ViewModel reads the record id from this route and pre-fills it.
                AddEditRecordScreen(
                    onBack = goBack,
                    onSaved = goBackWithMessage(entry),
                    onEditExisting = replaceWithEdit
                )
            }

            composable(
                route = Screen.RecordView.route,
                arguments = listOf(navArgument(Screen.ARG_RECORD_ID) { type = NavType.LongType })
            ) { entry ->
                RecordDetailScreen(
                    onBack = goBack,
                    onAddNew = addRecord,
                    onEdit = editRecord,
                    // After Delete, return to the list we came from (Dashboard, All Records, Search).
                    onDeleted = goBackWithMessage(entry)
                )
            }

            composable(
                route = Screen.SearchResults.route,
                arguments = listOf(
                    navArgument(Screen.ARG_START_DATE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(Screen.ARG_END_DATE) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument(Screen.ARG_SORT) {
                        type = NavType.StringType
                        defaultValue = SortOrder.NEWEST_FIRST.name
                    }
                )
            ) {
                // SearchViewModel reads startDate / endDate / sort from this route itself.
                SearchResultsScreen(
                    onBack = goBack,
                    onOpenRecord = openRecord,
                    onEditRecord = editRecord,
                    onShowMessage = showMessage
                )
            }
        }

        AppSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing) // stay above the navigation bar
        )
    }
}
