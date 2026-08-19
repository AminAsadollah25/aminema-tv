package com.amin.tvos

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amin.tvos.intro.IntroGate
import com.amin.tvos.intro.IntroOverlay
import com.amin.tvos.intro.IntroPreferences
import com.amin.tvos.browser.CatalogBackgroundSync
import com.amin.tvos.ui.home.HomeScreen
import com.amin.tvos.ui.home.HomeViewModel
import com.amin.tvos.ui.home.CatalogLibraryScreen
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.ui.settings.SettingsScreen
import com.amin.tvos.ui.theme.AminTvTheme
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    /** True only while the cold-start intro is on screen. */
    private var introVisible by mutableStateOf(false)
    private var autoCatalogSyncLaunched = false
    private lateinit var catalogSync: CatalogBackgroundSync

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A configuration recreation is not a new app opening.
        autoCatalogSyncLaunched = savedInstanceState != null

        val introPrefs = IntroPreferences(this)
        // Cold start only. A recreated activity — configuration change, or process death while
        // the browser was on top — must never replay the intro.
        introVisible = savedInstanceState == null &&
            introPrefs.playIntro &&
            IntroGate.consumeColdStart()
        val introMuted = introPrefs.muteIntro
        hideSystemBars()
        val app = application as AminTvApp
        catalogSync = CatalogBackgroundSync(this, app)

        setContent {
            AminTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Ink,
                    contentColor = TextPrimary
                ) {
                    Box(Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        val homeViewModel: HomeViewModel = viewModel()
                        val catalogSnapshot by homeViewModel.catalogSections.collectAsStateWithLifecycle()
                        var libraryItemsSelector by remember {
                            mutableStateOf<(List<com.amin.tvos.data.model.CatalogSection>) -> List<CatalogItem>>(
                                { emptyList() }
                            )
                        }
                        var libraryTitle by remember { mutableStateOf("کتابخانه") }
                        var libraryServices by remember { mutableStateOf<Set<String>>(emptySet()) }
                        var libraryOpenHandler by remember {
                            mutableStateOf<(CatalogItem) -> Unit>({})
                        }
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.fillMaxSize().background(Ink)
                        ) {
                            composable("home") {
                                // Refresh library when returning from the browser
                                val lifecycleOwner = LocalLifecycleOwner.current
                                DisposableEffect(lifecycleOwner) {
                                    val observer = LifecycleEventObserver { _, event ->
                                        if (event == Lifecycle.Event.ON_RESUME) homeViewModel.refresh()
                                    }
                                    lifecycleOwner.lifecycle.addObserver(observer)
                                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                                }
                                HomeScreen(
                                    onOpenSettings = { navController.navigate("settings") },
                                    onOpenLibrary = { title, itemsSelector, openItem, services ->
                                        libraryTitle = title
                                        libraryItemsSelector = itemsSelector
                                        libraryServices = services
                                        libraryOpenHandler = openItem
                                        navController.navigate("library")
                                    },
                                    onRefreshCatalog = { serviceId ->
                                        catalogSync.refresh(serviceId)
                                    },
                                    viewModel = homeViewModel
                                )
                            }
                            composable("library") {
                                CatalogLibraryScreen(
                                    title = libraryTitle,
                                    itemsProvider = { libraryItemsSelector(catalogSnapshot) },
                                    catalogRevision = catalogSnapshot,
                                    refreshingServices = homeViewModel.refreshingCatalogServices.collectAsStateWithLifecycle().value,
                                    providerIds = libraryServices,
                                    onRefresh = {
                                        libraryServices.forEach { serviceId ->
                                            catalogSync.refresh(serviceId)
                                        }
                                    },
                                    onLoadMore = { pageLimit, onFinished ->
                                        var remaining = libraryServices.size
                                        fun providerFinished() {
                                            remaining -= 1
                                            if (remaining <= 0) onFinished()
                                        }
                                        libraryServices.forEach { serviceId ->
                                            catalogSync.refresh(
                                                serviceId,
                                                pageLimit = pageLimit,
                                                onFinished = ::providerFinished
                                            )
                                        }
                                        if (remaining == 0) onFinished()
                                    },
                                    onBack = { navController.popBackStack() },
                                    onOpen = libraryOpenHandler
                                )
                            }
                            composable("settings") {
                                SettingsScreen(onBack = { navController.popBackStack() })
                            }
                        }

                        // Refresh provider caches once per real cold start. The tiny browser
                        // jobs start independently of the intro, run one provider at a time and
                        // never replace the screen or steal remote/mouse focus. This means the
                        // intro remains part of the Aminema identity while its duration becomes
                        // useful preload time instead of a hard wait before catalog work begins.
                        LaunchedEffect(Unit) {
                            if (!autoCatalogSyncLaunched) {
                                autoCatalogSyncLaunched = true
                                // HomeViewModel already loads the local services/library/catalog
                                // snapshot on creation. Wait only for services to be available so
                                // the first hidden WebView never races an empty service list.
                                app.servicesRepository.services.first { it.isNotEmpty() }

                                // First cold-start refresh is automatic but still independent:
                                // one provider finishes (or times out) before the next begins.
                                catalogSync.refresh(HomeViewModel.IRANIAN_SERVICE_ID)
                                while (
                                    HomeViewModel.IRANIAN_SERVICE_ID in
                                    app.catalogRepository.refreshingServices.value
                                ) {
                                    delay(250L)
                                }
                                catalogSync.refresh(HomeViewModel.INTERNATIONAL_SERVICE_ID)
                                while (
                                    HomeViewModel.INTERNATIONAL_SERVICE_ID in
                                    app.catalogRepository.refreshingServices.value
                                ) {
                                    delay(250L)
                                }
                                // MyMoviz catalogue/search is public. Login remains deferred
                                // until the separately-tested watch flow is implemented.
                                // MyMoviz is a large supplemental archive. One fresh page keeps
                                // cold start light; View All continues the cached window later.
                                catalogSync.refresh(
                                    HomeViewModel.MYMOVIZ_SERVICE_ID,
                                    pageLimit = 1
                                )
                                while (
                                    HomeViewModel.MYMOVIZ_SERVICE_ID in
                                    app.catalogRepository.refreshingServices.value
                                ) {
                                    delay(250L)
                                }
                            }
                        }

                        // Home is built underneath, so it is ready the moment the intro ends.
                        if (introVisible) {
                            IntroOverlay(
                                muted = introMuted,
                                onFinished = { introVisible = false }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (::catalogSync.isInitialized) catalogSync.destroy()
        super.onDestroy()
    }

    /**
     * The intro is modal: while it plays no key may reach Home, so the DPAD_CENTER that skipped
     * the intro cannot also open a service card. Volume keys still pass through.
     */
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!introVisible) return super.dispatchKeyEvent(event)
        if (event.keyCode in VOLUME_KEYS) return super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP && event.keyCode in SKIP_KEYS) {
            introVisible = false
        }
        return true
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private companion object {
        val SKIP_KEYS = setOf(
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_MEDIA_STOP
        )
        val VOLUME_KEYS = setOf(
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE
        )
    }
}
