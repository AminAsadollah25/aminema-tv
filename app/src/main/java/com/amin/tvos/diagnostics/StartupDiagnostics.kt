package com.amin.tvos.diagnostics

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.amin.tvos.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/** Timings only: never logs page addresses, content, account state or cookies. */
object StartupDiagnostics {
    private val origin = SystemClock.elapsedRealtime()
    private val watching = AtomicBoolean(false)

    fun mark(phase: String) {
        if (BuildConfig.DEBUG) {
            Log.d("AminStartup", "$phase elapsedMs=${SystemClock.elapsedRealtime() - origin}")
        }
    }

    /** Explicit debug profiling only. Read this app's last ANR, never dump a raw trace. */
    fun inspectPreviousAnr(context: android.content.Context, enabled: Boolean) {
        if (!enabled || !BuildConfig.DEBUG || android.os.Build.VERSION.SDK_INT < 30) return
        val appContext = context.applicationContext
        Thread({
            runCatching {
                val manager = appContext.getSystemService(android.app.ActivityManager::class.java)
                val exit = manager.getHistoricalProcessExitReasons(appContext.packageName, 0, 8)
                    .firstOrNull { it.reason == android.app.ApplicationExitInfo.REASON_ANR }
                    ?: return@runCatching
                val trace = exit.traceInputStream
                val frames = trace?.bufferedReader()?.useLines { lines ->
                    lines.take(16_000).dropWhile { !it.trimStart().startsWith("\"main\"") }.drop(1)
                        .takeWhile { it.isNotBlank() }
                        .map { it.trim() }
                        .filter { (it.startsWith("at ") || it.startsWith("native:")) && !it.contains("://") }
                        .take(40).map { it.take(300) }.toList()
                }.orEmpty()
                Log.d("AminAnr", "previousANR timestamp=${exit.timestamp} tracePresent=${trace != null} frames=${frames.size}")
                frames.forEach { Log.d("AminAnr", it) }
            }.onFailure { Log.d("AminAnr", "previousANR unavailable=${it.javaClass.simpleName}") }
        }, "AminAnrReader").apply { isDaemon = true }.start()
    }

    /** Opt-in sampling for `am start --ez profile_startup true`, not normal openings. */
    fun watchMainThread(enabled: Boolean) {
        if (!BuildConfig.DEBUG || !enabled || !watching.compareAndSet(false, true)) return
        val main = Looper.getMainLooper()
        val handler = Handler(main)
        Thread({
            val end = SystemClock.uptimeMillis() + 20_000L
            var samples = 0
            while (SystemClock.uptimeMillis() < end && samples < 40) {
                val answered = AtomicBoolean(false)
                handler.post { answered.set(true) }
                Thread.sleep(250L)
                if (!answered.get()) {
                    val stack = main.thread.stackTrace.take(12)
                        .joinToString(" <- ") { "${it.className}.${it.methodName}:${it.lineNumber}" }
                    Log.d("AminStartup", "mainStall250ms $stack")
                    samples++
                }
                Thread.sleep(100L)
            }
            watching.set(false)
        }, "AminStartupSampler").apply { isDaemon = true }.start()
    }
}
