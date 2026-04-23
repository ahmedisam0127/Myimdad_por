package com.myimdad_por.domain.repository

/**
 * Standard sort direction for repository queries.
 */
enum class SortDirection {
    ASCENDING,
    DESCENDING;

    val isAscending: Boolean
        get() = this == ASCENDING

    val isDescending: Boolean
        get() = this == DESCENDING
}