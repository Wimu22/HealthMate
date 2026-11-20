package com.example.healthmate.ui.navigation

import com.example.healthmate.domain.SortOrder

/**
 * Every destination in the app and its route string (spec section 6).
 *
 * A route is like a URL inside the app. `route` is the pattern registered in the NavHost
 * (with `{placeholders}` for arguments); `createRoute(...)` builds the real route to navigate to,
 * so the rest of the app never assembles route strings by hand.
 */
sealed class Screen(val route: String) {

    data object Dashboard : Screen("dashboard")

    data object AllRecords : Screen("all_records")

    data object AddRecord : Screen("add_record")

    data object EditRecord : Screen("edit_record/{$ARG_RECORD_ID}") {
        fun createRoute(recordId: Long) = "edit_record/$recordId"
    }

    data object RecordView : Screen("record_view/{$ARG_RECORD_ID}") {
        fun createRoute(recordId: Long) = "record_view/$recordId"
    }

    /**
     * Search Results, opened by Apply in the Filter bottom sheet.
     * The filters travel as optional query arguments: dates as `yyyy-MM-dd` (same format as the
     * database) and the sort order as its enum name. A missing date means "no limit on that side".
     */
    data object SearchResults : Screen(
        "search_results?$ARG_START_DATE={$ARG_START_DATE}&$ARG_END_DATE={$ARG_END_DATE}&$ARG_SORT={$ARG_SORT}"
    ) {
        fun createRoute(
            startDate: String? = null,
            endDate: String? = null,
            sortOrder: SortOrder = SortOrder.NEWEST_FIRST
        ): String = buildString {
            append("search_results?$ARG_SORT=${sortOrder.name}")
            if (startDate != null) append("&$ARG_START_DATE=$startDate")
            if (endDate != null) append("&$ARG_END_DATE=$endDate")
        }
    }

    companion object {
        const val ARG_RECORD_ID = "recordId"
        const val ARG_START_DATE = "startDate"
        const val ARG_END_DATE = "endDate"
        const val ARG_SORT = "sort"
    }
}
