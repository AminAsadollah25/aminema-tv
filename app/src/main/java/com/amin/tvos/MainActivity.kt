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
import com.amin.tvos.diagnostics.StartupDiagnostics
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import com.amin.tvos.browser.CatalogBootstrapPolicy
import com.amin.tvos.browser.CatalogBackgroundSync
import com.amin.tvos.ui.home.HomeScreen
import com.amin.tvos.ui.home.HomeViewModel
import com.amin.tvos.ui.home.CatalogLibraryScreen
import com.amin.tvos.ui.home.JourneyScreen
import com.amin.tvos.ui.spotlight.SpotlightActivity
import com.amin.tvos.data.model.SpotlightItem
import com.amin.tvos.data.model.CatalogKind
import com.amin.tvos.data.model.defaultSpotlightAction
import com.amin.tvos.data.model.CatalogItem
import com.amin.tvos.ui.settings.SettingsScreen
import com.amin.tvos.ui.theme.AminTvTheme
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    /** True only while the cold-start intro is on screen. */
    private var introVisible by mutableStateOf(false)
    private var autoCatalogSyncLaunched = false
    private var catalogSync: CatalogBackgroundSync? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        StartupDiagnostics.mark("main.create.start")
        StartupDiagnostics.watchMainThread(intent.getBooleanExtra("profile_startup", false))
        StartupDiagnostics.inspectPreviousAnr(this, intent.getBooleanExtra("profile_startup", false))
        super.onCreate(savedInstanceState)
        // A configuration recreation is not a new app opening.
        autoCatalogSyncLaunched = savedInstanceState?.getBoolean(CatalogBootstrapPolicy.SAVED_ATTEMPT) ?: false

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
                        LaunchedEffect(Unit) {
                            withFrameNanos { }
                            StartupDiagnostics.mark("compose.first.frame")
                        }
                        val catalogSnapshot by homeViewModel.fullCatalogSections.collectAsStateWithLifecycle()
                        val fullCatalogReady by homeViewModel.fullCatalogReady.collectAsStateWithLifecycle()
                        val configuredServices by homeViewModel.services.collectAsStateWithLifecycle()
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
                                val homeLifecycleState by lifecycleOwner.lifecycle.currentStateFlow
                                    .collectAsStateWithLifecycle(minActiveState = Lifecycle.State.CREATED)
                                LaunchedEffect(introVisible, homeLifecycleState, fullCatalogReady,
                                    configuredServices, catalogSnapshot) {
                                    fun missingIranianCache() = CatalogBootstrapPolicy.needsBootstrap(
                                        homeViewModel.fullCatalogReady.value, autoCatalogSyncLaunched,
                                        homeViewModel.services.value.map { it.id }.toSet(),
                                        homeViewModel.fullCatalogSections.value)
                                    if (introVisible || homeLifecycleState != Lifecycle.State.RESUMED ||
                                        !missingIranianCache()) return@LaunchedEffect
                                    // Render Home first; only the new public provider gets this
                                    // bounded first-page bootstrap. Existing providers do not scan.
                                    withFrameNanos { }
                                    delay(CatalogBootstrapPolicy.QUIET_DELAY_MS)
                                    val sync = catalogSync ?: return@LaunchedEffect
                                    if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.RESUMED ||
                                        !missingIranianCache()) return@LaunchedEffect
                                    autoCatalogSyncLaunched = true
                                    sync.refresh(CatalogBootstrapPolicy.SERVICE_ID,
                                        pageLimit = CatalogBootstrapPolicy.PAGE_LIMIT)
                                }
                                DisposableEffect(lifecycleOwner) {
                                    val observer = LifecycleEventObserver { _, event ->
                                        if (event == Lifecycle.Event.ON_RESUME) homeViewModel.refresh()
                                    }
                                    lifecycleOwner.lifecycle.addObserver(observer)
                                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                                }
                                // Keep cache loading in the ViewModel, but do not measure,
                                // animate and decode a whole hidden Home while VideoView is
                                // preparing/playing the modal intro. Skip still opens Home.
                                if (!introVisible) {
                                HomeScreen(
                                    onOpenSettings = { navController.navigate("settings") },
                                    onOpenGenreArchive = { category -> startActivity(
                                        com.amin.tvos.ui.home.GenreArchiveActivity.intent(this@MainActivity, category)) },
                                    onOpenJourney = { marvel -> navController.navigate(if (marvel) "marvel" else "marathon") },
                                    onOpenLibrary = { title, itemsSelector, openItem, services ->
                                        libraryTitle = title
                                        libraryItemsSelector = itemsSelector
                                        libraryServices = services
                                        libraryOpenHandler = openItem
                                        navController.navigate("library")
                                    },
                                    onRefreshCatalog = { serviceId ->
                                        catalogSync?.refresh(serviceId)
                                    },
                                    viewModel = homeViewModel
                                )
                                }
                            }
                            composable("library") {
                                CatalogLibraryScreen(
                                    title = libraryTitle,
                                    itemsProvider = { libraryItemsSelector(catalogSnapshot) },
                                    catalogRevision = catalogSnapshot,
                                    catalogReady = fullCatalogReady,
                                    refreshingServices = homeViewModel.refreshingCatalogServices.collectAsStateWithLifecycle().value,
                                    providerIds = libraryServices,
                                    onRefresh = {
                                        libraryServices.forEach { serviceId ->
                                            catalogSync?.refresh(serviceId)
                                        }
                                    },
                                    onLoadMore = { pageLimit, onFinished ->
                                        var remaining = libraryServices.size
                                        fun providerFinished() {
                                            remaining -= 1
                                            if (remaining <= 0) onFinished()
                                        }
                                        libraryServices.forEach { serviceId ->
                                            catalogSync?.refresh(
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
                            listOf("marvel", "marathon").forEach { route ->
                                composable(route) {
                                    val services by homeViewModel.services.collectAsStateWithLifecycle()
                                    val metadata by homeViewModel.titleMetadata.collectAsStateWithLifecycle()
                                    val journeyRepo = app.journeyLookupRepository
                                    val lookupRecords by journeyRepo.records.collectAsStateWithLifecycle()
                                    val lookupItems by androidx.compose.runtime.produceState<List<CatalogItem>>(emptyList(), lookupRecords) {
                                        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                                            com.amin.tvos.data.JourneyLookupPlanner.bestItems(lookupRecords)
                                        }
                                    }
                                    LaunchedEffect(Unit) { runCatching { journeyRepo.load() } }
                                    JourneyScreen(route == "marvel", catalogSnapshot, fullCatalogReady,
                                        metadata, services.associate { it.id to it.name },
                                        lookupItems = lookupItems,
                                        onDiscoverFamily = { familyId -> startActivity(android.content.Intent(this@MainActivity,
                                            com.amin.tvos.ui.home.MarathonDiscoveryActivity::class.java)
                                            .putExtra(com.amin.tvos.ui.home.MarathonDiscoveryActivity.FAMILY_ID, familyId)) },
                                        onMatchAll = { startActivity(android.content.Intent(this@MainActivity, com.amin.tvos.ui.home.JourneySyncActivity::class.java)) },
                                        onBack = { navController.popBackStack() }, onOpen = { media ->
                                            val item = media.representative
                                            startActivity(SpotlightActivity.intent(this@MainActivity, SpotlightItem(
                                                title = item.title, kind = item.kind, contentUrl = item.contentUrl,
                                                posterUrl = item.posterUrl, backdropUrl = item.backdropUrl,
                                                serviceId = item.serviceId, year = item.year, summary = item.summary,
                                                rating = item.rating, genres = item.genres, cast = item.cast, directors = item.directors,
                                                hasPersianDub = item.hasPersianDub, hasPersianSubtitle = item.hasPersianSubtitle,
                                                primaryAction = item.kind.defaultSpotlightAction(),
                                                directPlay = item.kind == CatalogKind.MOVIE && services.firstOrNull { it.id == item.serviceId }?.directPlay != null,
                                                canonicalId = media.canonicalId, sourceVariants = media.variants
                                            )))
                                        })
                                }
                            }
                        }

                        // General provider scans remain manual: their live WebViews previously
                        // monopolized the UI thread. Only a missing FarsiLand cache is bootstrapped
                        // above, after Home draws, using its inert public metadata host.

                        // Native data preloads during the intro; rich UI starts after it.
                        if (introVisible) {
                            IntroOverlay(
                                muted = introMuted,
                                onFinished = {
                                    StartupDiagnostics.mark("intro.finished")
                                    introVisible = false
                                }
                            )
                        }
                    }
                }
            }
        }
        StartupDiagnostics.mark("main.create.end")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(CatalogBootstrapPolicy.SAVED_ATTEMPT, autoCatalogSyncLaunched)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        // A visible browser/player or Live TV screen must not share the main thread with a
        // hidden catalog WebView left behind by Home. Recreate the tiny sync host when Home
        // returns; cached catalog data remains available and manual refresh still works.
        if (catalogSync == null) {
            catalogSync = CatalogBackgroundSync(this, application as AminTvApp)
        }
    }

    override fun onPause() {
        catalogSync?.destroy()
        catalogSync = null
        super.onPause()
    }

    override fun onDestroy() {
        catalogSync?.destroy()
        catalogSync = null
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
            StartupDiagnostics.mark("intro.skipped")
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
