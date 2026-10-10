package com.amin.tvos

import android.app.Application
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val liveCoordinator = lazy { LiveChannelHealthCoordinator(this) }
    val liveChannelHealthCoordinator get() = liveCoordinator.value

    override fun onCreate() {
        super.onCreate()
        // Lifecycle belongs to the app, not LiveTvActivity. Debounce the short gap
        // between two Activities; going Home or opening a channel retains the queue.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var started = 0
            private val handler = Handler(Looper.getMainLooper())
            private val pauseScan = Runnable {
                if (started == 0 && liveCoordinator.isInitialized()) {
                    liveCoordinator.value.setAppVisible(false)
                }
            }
            override fun onActivityStarted(activity: Activity) {
                started += 1
                handler.removeCallbacks(pauseScan)
                if (liveCoordinator.isInitialized()) liveCoordinator.value.setAppVisible(true)
            }
            override fun onActivityStopped(activity: Activity) {
                started = (started - 1).coerceAtLeast(0)
                if (started == 0) handler.postDelayed(pauseScan, 500L)
            }
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        // Do not initialise Chromium before the first native frame. Every provider
        // WebView enables cookies before navigation; existing persistent cookies stay intact.
    }

    override fun onTerminate() {
        if (liveCoordinator.isInitialized()) liveCoordinator.value.close()
        super.onTerminate()
    }
}
