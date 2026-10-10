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
