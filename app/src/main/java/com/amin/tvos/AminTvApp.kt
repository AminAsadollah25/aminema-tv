package com.amin.tvos

import android.app.Application
import android.webkit.CookieManager
import android.webkit.WebView
import com.amin.tvos.BuildConfig
import com.amin.tvos.data.CatalogRepository
import com.amin.tvos.data.LiveChannelHealthCoordinator
import com.amin.tvos.data.LibraryRepository
import com.amin.tvos.data.ServicesRepository
import com.amin.tvos.data.SettingsRepository
import com.amin.tvos.update.UpdateRepository

/**
 * Application + lightweight service locator.
 * Kept dependency-injection-framework-free for fast startup on Android boxes.
 */
class AminTvApp : Application() {

    val servicesRepository by lazy { ServicesRepository(this) }
    val libraryRepository by lazy { LibraryRepository(this) }
    val catalogRepository by lazy { CatalogRepository(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val updateRepository by lazy { UpdateRepository(this) }
    val liveChannelHealthCoordinator by lazy { LiveChannelHealthCoordinator(this) }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)
        // Persistent login sessions: accept + persist cookies globally.
        CookieManager.getInstance().setAcceptCookie(true)
    }

    override fun onTerminate() {
        liveChannelHealthCoordinator.close()
        super.onTerminate()
    }
}
