package com.amin.tvos.data.model

/** One search hit — always a normal detail page of the user's own service. */
data class SearchResult(
    val title: String,
    val kind: CatalogKind,
    val contentUrl: String,
    val posterUrl: String,
    val serviceId: String,
    /** Release year only when the provider's visible result already includes it. */
    val year: String = ""
)

/** Per-service state of a single query, so one slow or broken site never blocks the other. */
data class SearchGroup(
    val serviceId: String,
    val results: List<SearchResult> = emptyList(),
    val loading: Boolean = false,
    val error: String = "",
    val searched: Boolean = false
)
