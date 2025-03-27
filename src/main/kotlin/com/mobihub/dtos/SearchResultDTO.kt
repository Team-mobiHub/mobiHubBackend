package com.mobihub.dtos

import kotlinx.serialization.Serializable

/**
 * Data transfer object for search results.
 *
 * @property searchResult The list of search result items.
 * @property totalCount The total number of search results.
 *
 * @author Team-MobiHub
 */
@Serializable
data class SearchResultDTO (
    val searchResult: List<SearchResultItemDTO>,
    val totalCount: Long
)