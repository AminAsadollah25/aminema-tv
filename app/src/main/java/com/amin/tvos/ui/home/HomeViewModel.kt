package com.amin.tvos.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.CatalogFilter
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.MovieItem
import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.StreamingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val tvApp = app as AminTvApp
    private val servicesRepo = tvApp.servicesRepository
    private val libraryRepo = tvApp.libraryRepository
    private val catalogRepo = tvApp.catalogRepository
    private val settingsRepo = tvApp.settingsRepository

    val services: StateFlow<List<StreamingService>> = servicesRepo.services

    // ---- "Latest" rows ----

    val catalogSections: StateFlow<List<CatalogSection>> = catalogRepo.sections
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun section(serviceId: String): CatalogSection? =
        catalogSections.value.firstOrNull { it.serviceId == serviceId }

    private fun filterFlow(serviceId: String): StateFlow<CatalogFilter> =
        settingsRepo.catalogFilter(serviceId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, CatalogFilter.ALL)

    val iranianFilter: StateFlow<CatalogFilter> = filterFlow(IRANIAN_SERVICE_ID)
    val internationalFilter: StateFlow<CatalogFilter> = filterFlow(INTERNATIONAL_SERVICE_ID)

    fun setCatalogFilter(serviceId: String, filter: CatalogFilter) = viewModelScope.launch {
        settingsRepo.setCatalogFilter(serviceId, filter)
    }

    val continueWatching: StateFlow<List<PlaybackSession>> =
        libraryRepo.playbackSessions
            .combine(servicesRepo.services) { sessions, configuredServices ->
                libraryRepo.continueWatching(sessions).map { session ->
                    session.copy(
                        serviceName = configuredServices.firstOrNull {
                            it.id == session.serviceId
                        }?.name ?: session.serviceName
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentlyOpened: StateFlow<List<MovieItem>> = libraryRepo.items
        .combine(servicesRepo.services) { list, configuredServices ->
            list.asSequence()
                .filterNot { item ->
                    val service = configuredServices.firstOrNull {
                        it.id == item.serviceId
                    }
                    when {
                        service == null -> genericExcluded(item.url)
                        isServiceRoot(item.url, service.url) -> true
                        service.playbackUrlPatterns.any { matches(it, item.url) } -> true
                        service.excludedUrlPatterns.any { matches(it, item.url) } -> true
                        service.contentUrlPatterns.isNotEmpty() ->
                            service.contentUrlPatterns.none { matches(it, item.url) }
                        else -> genericExcluded(item.url)
                    }
                }
                .map { item ->
                    item.copy(
                        serviceName = configuredServices.firstOrNull {
                            it.id == item.serviceId
                        }?.name ?: item.serviceName
                    )
                }
                .sortedByDescending { it.lastOpened }
                .take(20)
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<MovieItem>> = libraryRepo.items
        .combine(servicesRepo.services) { list, configuredServices ->
            list.filter { it.isFavorite }.map { item ->
                item.copy(
                    serviceName = configuredServices.firstOrNull {
                        it.id == item.serviceId
                    }?.name ?: item.serviceName
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        servicesRepo.load()
        libraryRepo.load()
        catalogRepo.load()
    }

    fun toggleFavorite(id: String) = viewModelScope.launch {
        libraryRepo.toggleFavorite(id)
    }

    private fun matches(pattern: String, value: String): Boolean =
        runCatching {
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(value)
        }.getOrDefault(false)

    private fun genericExcluded(url: String): Boolean =
        Regex(
            """(?:^|/)(?:login|signin|sign-in|auth|account|profile|settings)(?:/|$)""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(url)

    companion object {
        /** Internal adapter ids; Home only ever shows «ایرانی» and «خارجی». */
        const val IRANIAN_SERVICE_ID = "parsiflix"
        const val INTERNATIONAL_SERVICE_ID = "filmrooz"
    }

    private fun isServiceRoot(current: String, home: String): Boolean =
        runCatching {
            val currentUri = android.net.Uri.parse(current)
            val homeUri = android.net.Uri.parse(home)
            currentUri.host.equals(homeUri.host, ignoreCase = true) &&
                currentUri.path.orEmpty().trimEnd('/').ifBlank { "/" } ==
                homeUri.path.orEmpty().trimEnd('/').ifBlank { "/" } &&
                currentUri.query.isNullOrBlank()
        }.getOrDefault(false)
}
