package com.amin.tvos.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amin.tvos.AminTvApp
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

    val services: StateFlow<List<StreamingService>> = servicesRepo.services

    val continueWatching: StateFlow<List<PlaybackSession>> =
        libraryRepo.playbackSessions
        .map { libraryRepo.continueWatching(it) }
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
                .sortedByDescending { it.lastOpened }
                .take(20)
                .toList()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<MovieItem>> = libraryRepo.items
        .map { list -> list.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        servicesRepo.load()
        libraryRepo.load()
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
