package com.amin.tvos.intro

/**
 * One-shot, process-scoped gate for the Aminema intro.
 *
 * The intro belongs to a cold start only. Returning from [com.amin.tvos.browser.BrowserActivity],
 * [com.amin.tvos.browser.AccountSyncActivity] or Settings reuses the same process, so the gate is
 * already spent by then and Home shows immediately.
 */
object IntroGate {

    private var consumed = false

    /** Returns true exactly once per process. */
    @Synchronized
    fun consumeColdStart(): Boolean {
        if (consumed) return false
        consumed = true
        return true
    }
}
