package com.amin.tvos.browser

import com.amin.tvos.data.model.CatalogSection

/** A bounded empty-cache bootstrap, not a general cold-start provider scan. */
object CatalogBootstrapPolicy {
    const val SERVICE_ID = "farsiland"
    const val PAGE_LIMIT = 1
    const val QUIET_DELAY_MS = 1_500L
    const val SAVED_ATTEMPT = "farsiland_bootstrap_attempted"

    fun needsBootstrap(
        cacheReady: Boolean,
        attempted: Boolean,
        serviceIds: Set<String>,
        sections: List<CatalogSection>
    ): Boolean {
        if (!cacheReady || attempted || SERVICE_ID !in serviceIds) return false
        val section = sections.firstOrNull { it.serviceId == SERVICE_ID }
        return section == null ||
            (section.all.isEmpty() && section.movies.isEmpty() && section.series.isEmpty())
    }
}
