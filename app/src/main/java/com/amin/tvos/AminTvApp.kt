package com.amin.tvos

import android.app.Application
import android.webkit.CookieManager
import com.amin.tvos.data.CatalogRepository
import com.amin.tvos.data.LibraryRepository
import com.amin.tvos.data.ServicesRepository
import com.amin.tvos.data.SettingsRepository

/**
 * Application + lightweight service locator.
 * Kept dependency-injection-framework-free for fast startup on Android boxes.
 */
class AminTvApp : Application() {

    val servicesRepository by lazy { ServicesRepository(this) }
    val libraryRepository by lazy { LibraryRepository(this) }
    val catalogRepository by lazy { CatalogRepository(this) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // Persistent login sessions: accept + persist cookies globally.
        CookieManager.getInstance().setAcceptCookie(true)
    }
}
