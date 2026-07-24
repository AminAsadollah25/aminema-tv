package com.amin.tvos.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.MovieItem
import com.amin.tvos.data.model.StreamingService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val tvApp = app as AminTvApp
    private val servicesRepo = tvApp.servicesRepository
    private val libraryRepo = tvApp.libraryRepository

    val services: StateFlow<List<StreamingService>> = servicesRepo.services

    val continueWatching: StateFlow<List<MovieItem>> = libraryRepo.items
        .map { libraryRepo.continueWatching(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentlyOpened: StateFlow<List<MovieItem>> = libraryRepo.items
        .map { list ->
            list.asSequence()
                .filterNot { item ->
                    Regex(
                        """(?:^|/)(?:login|signin|sign-in|auth|account|profile|settings)(?:/|$)""",
                        RegexOption.IGNORE_CASE
                    ).containsMatchIn(item.url)
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
}
