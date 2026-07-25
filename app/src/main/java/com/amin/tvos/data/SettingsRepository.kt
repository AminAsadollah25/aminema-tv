package com.amin.tvos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.amin.tvos.data.model.CatalogFilter
import com.amin.tvos.data.model.UserAgentMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** App settings — currently the browser User-Agent mode. Remembered across launches. */
class SettingsRepository(private val context: Context) {

    private val uaKey = stringPreferencesKey("user_agent_mode")

    val userAgentMode: Flow<UserAgentMode> = context.dataStore.data.map { prefs ->
        runCatching { UserAgentMode.valueOf(prefs[uaKey] ?: UserAgentMode.TV.name) }
            .getOrDefault(UserAgentMode.TV)
    }

    suspend fun setUserAgentMode(mode: UserAgentMode) {
        context.dataStore.edit { it[uaKey] = mode.name }
    }

    // ---- Login / QR page zoom (percent, default 85) ----

    private val zoomKey = intPreferencesKey("browser_zoom")

    companion object {
        const val DEFAULT_BROWSER_ZOOM = 85
        const val MIN_BROWSER_ZOOM = 50
        const val MAX_BROWSER_ZOOM = 120
    }

    val browserZoom: Flow<Int> = context.dataStore.data.map { prefs ->
        (prefs[zoomKey] ?: DEFAULT_BROWSER_ZOOM).coerceIn(MIN_BROWSER_ZOOM, MAX_BROWSER_ZOOM)
    }

    suspend fun setBrowserZoom(percent: Int) {
        context.dataStore.edit {
            it[zoomKey] = percent.coerceIn(MIN_BROWSER_ZOOM, MAX_BROWSER_ZOOM)
        }
    }

    // ---- Remembered "همه | فیلم | سریال" choice per latest row ----

    private fun catalogFilterKey(serviceId: String) =
        stringPreferencesKey("catalog_filter_$serviceId")

    fun catalogFilter(serviceId: String): Flow<CatalogFilter> =
        context.dataStore.data.map { prefs ->
            runCatching {
                CatalogFilter.valueOf(
                    prefs[catalogFilterKey(serviceId)] ?: CatalogFilter.ALL.name
                )
            }.getOrDefault(CatalogFilter.ALL)
        }

    suspend fun setCatalogFilter(serviceId: String, filter: CatalogFilter) {
        context.dataStore.edit { it[catalogFilterKey(serviceId)] = filter.name }
    }
}
