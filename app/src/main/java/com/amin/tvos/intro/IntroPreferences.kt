package com.amin.tvos.intro

import android.content.Context

/**
 * "Play intro" / "Mute intro" flags.
 *
 * Deliberately backed by SharedPreferences instead of the DataStore used by
 * [com.amin.tvos.data.SettingsRepository]: the decision has to be made synchronously inside
 * `MainActivity.onCreate`, before the first frame. An async read would either flash Home first or
 * block the main thread at cold start.
 */
class IntroPreferences(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var playIntro: Boolean
        get() = prefs.getBoolean(KEY_PLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_PLAY, value).apply()

    var muteIntro: Boolean
        get() = prefs.getBoolean(KEY_MUTE, false)
        set(value) = prefs.edit().putBoolean(KEY_MUTE, value).apply()

    private companion object {
        const val FILE_NAME = "intro_prefs"
        const val KEY_PLAY = "play_intro"
        const val KEY_MUTE = "mute_intro"
    }
}
