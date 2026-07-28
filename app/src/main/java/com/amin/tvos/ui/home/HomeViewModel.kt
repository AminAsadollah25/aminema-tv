package com.amin.tvos.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.BuildConfig
import com.amin.tvos.data.model.CatalogFilter
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.CatalogSection
import com.amin.tvos.data.model.catalogKindFromUrl
import com.amin.tvos.data.model.MovieItem
import com.amin.tvos.data.model.PlaybackSession
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.update.ReleaseInfo
import com.amin.tvos.update.UpdateState
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val tvApp = app as AminTvApp
    private val servicesRepo = tvApp.servicesRepository
    private val libraryRepo = tvApp.libraryRepository
    private val catalogRepo = tvApp.catalogRepository
    private val settingsRepo = tvApp.settingsRepository
    private val updateRepo = tvApp.updateRepository

    val services: StateFlow<List<StreamingService>> = servicesRepo.services

    // ---- "Latest" rows ----

    val catalogSections: StateFlow<List<CatalogSection>> = catalogRepo.sections
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val refreshingCatalogServices: StateFlow<Set<String>> =
        catalogRepo.refreshingServices
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

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

    /** Used by the greeting's "show me movies / series" suggestion. */
    fun setAllCatalogFilters(filter: CatalogFilter) = viewModelScope.launch {
        settingsRepo.setCatalogFilter(IRANIAN_SERVICE_ID, filter)
        settingsRepo.setCatalogFilter(INTERNATIONAL_SERVICE_ID, filter)
    }

    /** One random title from the cached rows, for the greeting's "surprise me". */
    fun randomCatalogItem(): CatalogItem? =
        catalogSections.value.flatMap { it.all }.randomOrNull()

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

    /**
     * Title-level membership comes from the signed-in account Recent row plus local Aminema
     * sessions. Episode progress is intentionally not invented when the provider only knows the
     * title: Home shows release metadata until exact local playback is available.
     */
    val mySeries: StateFlow<List<PlaybackSession>> =
        libraryRepo.playbackSessions
            .combine(servicesRepo.services) { sessions, configuredServices ->
                sessions.asSequence()
                    .filter { session ->
                        catalogKindFromUrl(session.contentUrl) == CatalogKind.SERIES
                    }
                    .distinctBy { it.contentUrl.trimEnd('/') }
                    .sortedByDescending { it.lastPlayed }
                    .map { session ->
                        session.copy(
                            serviceName = configuredServices.firstOrNull {
                                it.id == session.serviceId
                            }?.name ?: session.serviceName
                        )
                    }
                    .take(20)
                    .toList()
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

    // ---- Self-update ----

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        refresh()
        checkForUpdate()
    }

    fun refresh() = viewModelScope.launch {
        servicesRepo.load()
        libraryRepo.load()
        catalogRepo.load()
    }

    /** Cold-start check, silent unless a release newer than any previously skipped one exists. */
    fun checkForUpdate() = viewModelScope.launch {
        val release = updateRepo.checkForUpdate(BuildConfig.VERSION_CODE) ?: return@launch
        val skipped = settingsRepo.skippedUpdateVersionCode.first()
        if (release.versionCode > skipped) _updateState.value = UpdateState.Available(release)
    }

    /** Settings' manual "check now" — ignores any previously skipped version. */
    fun checkForUpdateManually() = viewModelScope.launch {
        _updateState.value = UpdateState.Checking
        val release = updateRepo.checkForUpdate(BuildConfig.VERSION_CODE)
        _updateState.value = if (release != null) {
            UpdateState.Available(release)
        } else {
            UpdateState.Idle
        }
    }

    fun skipUpdate(release: ReleaseInfo) = viewModelScope.launch {
        settingsRepo.setSkippedUpdateVersionCode(release.versionCode)
        _updateState.value = UpdateState.Idle
    }

    fun downloadAndInstall(release: ReleaseInfo, context: Context) = viewModelScope.launch {
        if (!updateRepo.canRequestInstalls()) {
            context.startActivity(
                updateRepo.unknownSourcesSettingsIntent()
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return@launch
        }
        _updateState.value = UpdateState.Downloading(release, 0)
        val file: File = try {
            updateRepo.download(release) { percent ->
                _updateState.value = UpdateState.Downloading(release, percent)
            }
        } catch (error: Exception) {
            _updateState.value = UpdateState.Failed(release, error.message ?: "Download failed")
            return@launch
        }
        context.startActivity(updateRepo.installIntent(file))
        _updateState.value = UpdateState.Idle
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
