package com.amin.tvos.ui.settings

import android.app.Application
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amin.tvos.AminTvApp
import com.amin.tvos.data.model.ServiceType
import com.amin.tvos.data.model.StreamingService
import com.amin.tvos.data.model.UserAgentMode
import com.amin.tvos.intro.IntroPreferences
import com.amin.tvos.update.ReleaseInfo
import com.amin.tvos.update.UpdateState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val tvApp = app as AminTvApp
    private val servicesRepo = tvApp.servicesRepository
    private val settingsRepo = tvApp.settingsRepository
    private val libraryRepo = tvApp.libraryRepository
    private val introPrefs = IntroPreferences(app)
    private val updateRepo = tvApp.updateRepository

    val updateState: StateFlow<UpdateState> = updateRepo.state

    val services: StateFlow<List<StreamingService>> = servicesRepo.services

    val userAgentMode: StateFlow<UserAgentMode> = settingsRepo.userAgentMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserAgentMode.TV)

    fun setUserAgent(mode: UserAgentMode) = viewModelScope.launch {
        settingsRepo.setUserAgentMode(mode)
    }

    val browserZoom: StateFlow<Int> = settingsRepo.browserZoom
        .stateIn(
            viewModelScope, SharingStarted.Eagerly,
            com.amin.tvos.data.SettingsRepository.DEFAULT_BROWSER_ZOOM
        )

    fun changeBrowserZoom(delta: Int) = viewModelScope.launch {
        settingsRepo.setBrowserZoom(browserZoom.value + delta)
    }

    fun resetBrowserZoom() = viewModelScope.launch {
        settingsRepo.setBrowserZoom(com.amin.tvos.data.SettingsRepository.DEFAULT_BROWSER_ZOOM)
    }

    // ---- Cold-start intro ----
    // Backed by SharedPreferences (see IntroPreferences), mirrored here so Settings recomposes.

    private val _playIntro = MutableStateFlow(introPrefs.playIntro)
    val playIntro: StateFlow<Boolean> = _playIntro.asStateFlow()

    private val _muteIntro = MutableStateFlow(introPrefs.muteIntro)
    val muteIntro: StateFlow<Boolean> = _muteIntro.asStateFlow()

    fun setPlayIntro(enabled: Boolean) {
        introPrefs.playIntro = enabled
        _playIntro.value = enabled
    }

    fun setMuteIntro(muted: Boolean) {
        introPrefs.muteIntro = muted
        _muteIntro.value = muted
    }

    fun addService(name: String, url: String) = viewModelScope.launch {
        if (name.isBlank() || url.isBlank()) return@launch
        val normalized = if (url.startsWith("http")) url else "https://$url"
        val nameId = name.lowercase().replace(Regex("[^a-z0-9]"), "_").trim('_')
        val hostId = runCatching { Uri.parse(normalized).host.orEmpty() }
            .getOrDefault("")
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "_")
            .trim('_')
        servicesRepo.addService(
            StreamingService(
                id = nameId.ifBlank { hostId }.ifBlank { "service_${System.currentTimeMillis()}" },
                name = name.trim(),
                url = normalized.trim(),
                type = ServiceType.STREAMING
            )
        )
    }

    fun removeService(id: String) = viewModelScope.launch { servicesRepo.removeService(id) }

    /** Logout everywhere: cookies + web storage. */
    fun clearCookies(onDone: () -> Unit) {
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            WebStorage.getInstance().deleteAllData()
            onDone()
        }
    }

    fun clearCache(onDone: () -> Unit) = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            getApplication<Application>().cacheDir.deleteRecursively()
        }
        onDone()
    }

    fun clearHistory() = viewModelScope.launch { libraryRepo.clearHistory() }

    /** Manual check publishes into the application-scoped banner shared with Home. */
    fun checkForUpdate(onResult: (ReleaseInfo?) -> Unit) =
        viewModelScope.launch {
            updateRepo.publishState(UpdateState.Checking)
            val release = updateRepo.checkForUpdate(
                com.amin.tvos.BuildConfig.VERSION_CODE,
                com.amin.tvos.BuildConfig.VERSION_NAME
            )
            updateRepo.publishState(
                if (release != null) UpdateState.Available(release) else UpdateState.Idle
            )
            onResult(release)
        }

    fun skipUpdate(release: ReleaseInfo) = viewModelScope.launch {
        settingsRepo.setSkippedUpdateVersionCode(release.versionCode)
        updateRepo.publishState(UpdateState.Idle)
    }

    fun downloadAndInstall(release: ReleaseInfo) = viewModelScope.launch {
        val context = getApplication<Application>()
        if (!updateRepo.canRequestInstalls()) {
            context.startActivity(
                updateRepo.unknownSourcesSettingsIntent()
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return@launch
        }

        updateRepo.publishState(UpdateState.Downloading(release, 0))
        val file = try {
            updateRepo.download(release) { percent ->
                updateRepo.publishState(UpdateState.Downloading(release, percent))
            }
        } catch (error: Exception) {
            updateRepo.publishState(
                UpdateState.Failed(release, error.message ?: "Download failed")
            )
            return@launch
        }
        context.startActivity(updateRepo.installIntent(file))
        updateRepo.publishState(UpdateState.Idle)
    }
}
