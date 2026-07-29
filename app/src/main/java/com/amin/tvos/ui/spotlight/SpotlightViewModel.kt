package com.amin.tvos.ui.spotlight

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.ContentMetadataPolicy
import com.amin.tvos.data.model.SpotlightItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpotlightViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as AminTvApp).libraryRepository

    val favoriteUrls: StateFlow<Set<String>> = repository.items
        .map { items ->
            items
                .filter { it.isFavorite }
                .mapTo(mutableSetOf()) {
                    ContentMetadataPolicy.canonicalContentUrl(it.url)
                }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        viewModelScope.launch { repository.load() }
    }

    fun toggleFavorite(item: SpotlightItem) {
        viewModelScope.launch {
            repository.toggleFavoriteForPage(
                serviceId = item.serviceId,
                serviceName = item.serviceName,
                url = item.contentUrl,
                title = item.title,
                posterUrl = item.posterUrl,
                resumePosition = item.resumePosition,
                duration = item.duration,
                isPlayable = item.kind == com.amin.tvos.data.model.CatalogKind.MOVIE
            )
        }
    }
}
