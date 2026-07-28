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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amin.tvos.intro.IntroGate
import com.amin.tvos.intro.IntroOverlay
import com.amin.tvos.intro.IntroPreferences
import com.amin.tvos.browser.AccountSyncActivity
import com.amin.tvos.ui.home.HomeScreen
import com.amin.tvos.ui.home.HomeViewModel
import com.amin.tvos.ui.settings.SettingsScreen
import com.amin.tvos.ui.theme.AminTvTheme
import com.amin.tvos.ui.theme.Ink
import com.amin.tvos.ui.theme.TextPrimary
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    /** True only while the cold-start intro is on screen. */
    private var introVisible by mutableStateOf(false)
    private var autoAccountSyncLaunched = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val introPrefs = IntroPreferences(this)
        // Cold start only. A recreated activity — configuration change, or process death while
        // the browser was on top — must never replay the intro.
        introVisible = savedInstanceState == null &&
            introPrefs.playIntro &&
            IntroGate.consumeColdStart()
        val introMuted = introPrefs.muteIntro
        hideSystemBars()

        setContent {
            AminTvTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Ink,
                    contentColor = TextPrimary
                ) {
                    Box(Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = "home",
                            modifier = Modifier.fillMaxSize().background(Ink)
                        ) {
                            composable("home") {
                                val vm: HomeViewModel = viewModel()
                                // Refresh library when returning from the browser
                                val lifecycleOwner = LocalLifecycleOwner.current
                                DisposableEffect(lifecycleOwner) {
                                    val observer = LifecycleEventObserver { _, event ->
                                        if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
                                    }
                                    lifecycleOwner.lifecycle.addObserver(observer)
                                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                                }
                                HomeScreen(
                                    onOpenSettings = { navController.navigate("settings") },
                                    viewModel = vm
                                )
                            }
                            composable("settings") {
                                SettingsScreen(onBack = { navController.popBackStack() })
                            }
                        }

                        // Refresh the website-account Continue rows once per cold start
                        // (at most every 15 minutes). This makes emulator and TV converge
                        // without requiring the user to remember the Sync button.
                        LaunchedEffect(introVisible) {
                            if (
                                !introVisible &&
                                !autoAccountSyncLaunched &&
                                AccountSyncActivity.acquireAutoSync(this@MainActivity)
                            ) {
                                autoAccountSyncLaunched = true
                                delay(900L)
                                startActivity(
                                    android.content.Intent(
                                        this@MainActivity,
                                        AccountSyncActivity::class.java
                                    )
                                )
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
