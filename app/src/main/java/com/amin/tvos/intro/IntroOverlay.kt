package com.amin.tvos.intro

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amin.tvos.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay

/** Give up if the decoder never reports "prepared" (weak box, broken file). */
private const val PREPARE_TIMEOUT_MS = 4_000L

/** Hard cap so a stalled decoder can never hold Home hostage. */
private const val MAX_PLAYBACK_MS = 20_000L

/**
 * Full-screen cold-start intro.
 *
 * Plays a local, offline video from `res/raw` exactly once — no network, no looping. Any decoder
 * error, missing file or stall falls through to [onFinished] so Home always opens.
 *
 * Skipping via OK / Enter / DPAD_CENTER / Back is handled by `MainActivity.dispatchKeyEvent`
 * (the intro is modal, so no key may leak into Home); a mouse click is handled here.
 */
@Composable
fun IntroOverlay(
    muted: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestOnFinished by rememberUpdatedState(onFinished)
    val done = remember { AtomicBoolean(false) }
    val finishOnce = remember {
        { if (done.compareAndSet(false, true)) latestOnFinished() }
    }
    val started = remember { mutableStateOf(false) }

    val videoUri = remember(context) {
        Uri.parse("android.resource://${context.packageName}/${R.raw.aminema_intro}")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { finishOnce() }
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setBackgroundColor(android.graphics.Color.BLACK)
                    keepScreenOn = true
                    // AndroidView receives pointer events before the Compose parent, so handle
                    // mouse / air-mouse clicks on the native view as well as on the overlay.
                    isClickable = true
                    setOnClickListener { finishOnce() }
                    setOnTouchListener { view, event ->
                        if (event.action == android.view.MotionEvent.ACTION_UP) {
                            view.performClick()
                        }
                        true
                    }
                    setOnPreparedListener { player ->
                        player.isLooping = false
                        // Audio otherwise follows the device media volume.
                        if (muted) player.setVolume(0f, 0f)
                        started.value = true
                        // The black VideoView background would stay on top of the first
                        // frames on some boxes.
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                    setOnCompletionListener { finishOnce() }
                    setOnErrorListener { _, _, _ ->
                        finishOnce()
                        true
                    }
                    runCatching {
                        setVideoURI(videoUri)
                        start()
                    }.onFailure { finishOnce() }
                }
            },
            onRelease = { it.stopPlayback() }
        )
    }

    // Watchdogs: never let a broken decoder block the app.
    LaunchedEffect(Unit) {
        delay(PREPARE_TIMEOUT_MS)
        if (!started.value) finishOnce()
    }
    LaunchedEffect(Unit) {
        delay(MAX_PLAYBACK_MS)
        finishOnce()
    }

    // If the user leaves the app mid-intro, drop it instead of resuming a half-played
    // video (and instead of waiting for the watchdog) — they come back straight to Home.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) finishOnce()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            done.set(true)
        }
    }
}
