package com.example.healthmate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthmate.domain.SortOrder
import com.example.healthmate.ui.dashboard.DashboardScreen

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

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
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
            PlaceholderScreen(
                title = "All Records",
                onBack = goBack,
                actions = listOf(
                    PlaceholderAction("Open record $SAMPLE_RECORD_ID") { openRecord(SAMPLE_RECORD_ID) },
                    PlaceholderAction("Edit record $SAMPLE_RECORD_ID") { editRecord(SAMPLE_RECORD_ID) },
                    PlaceholderAction("Add New Record", addRecord)
                )
            )
        }

        composable(Screen.AddRecord.route) {
            PlaceholderScreen(
                title = "Add New Record",
                onBack = goBack,
                actions = listOf(
                    PlaceholderAction("Save Record", goBack),
                    PlaceholderAction("Cancel", goBack)
                )
            )
        }

        composable(
            route = Screen.EditRecord.route,
            arguments = listOf(navArgument(Screen.ARG_RECORD_ID) { type = NavType.LongType })
        ) { entry ->
            val recordId = entry.arguments?.getLong(Screen.ARG_RECORD_ID) ?: 0L
            PlaceholderScreen(
                title = "Edit Record",
                details = "Record id: $recordId",
                onBack = goBack,
                actions = listOf(
                    PlaceholderAction("Update Record", goBack),
                    PlaceholderAction("Cancel", goBack)
                )
            )
        }

        composable(
            route = Screen.RecordView.route,
            arguments = listOf(navArgument(Screen.ARG_RECORD_ID) { type = NavType.LongType })
        ) { entry ->
            val recordId = entry.arguments?.getLong(Screen.ARG_RECORD_ID) ?: 0L
            PlaceholderScreen(
                title = "Record View",
                details = "Record id: $recordId",
                onBack = goBack,
                actions = listOf(
                    PlaceholderAction("Add New", addRecord),
                    PlaceholderAction("Edit") { editRecord(recordId) },
                    // After Delete, return to the list we came from.
                    PlaceholderAction("Delete", goBack)
                )
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
        ) { entry ->
            val args = entry.arguments
            val startDate = args?.getString(Screen.ARG_START_DATE)
            val endDate = args?.getString(Screen.ARG_END_DATE)
            val sort = args?.getString(Screen.ARG_SORT) ?: SortOrder.NEWEST_FIRST.name
            PlaceholderScreen(
                title = "Search Results",
                details = "From: ${startDate ?: "any"}\nTo: ${endDate ?: "any"}\nSort: $sort",
                onBack = goBack,
                actions = listOf(
                    PlaceholderAction("Open record $SAMPLE_RECORD_ID") { openRecord(SAMPLE_RECORD_ID) },
                    PlaceholderAction("Reset filters", goBack)
                )
            )
        }
    }
}

/** Placeholder-only: seeded data gives the first record id 1. Removed once real lists exist. */
private const val SAMPLE_RECORD_ID = 1L
