package com.amin.tvos.update

/** One published GitHub release, reduced to what the updater needs. */
data class ReleaseInfo(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val apkFileName: String,
    val sha256Url: String?,
    val notes: String
)

sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Available(val release: ReleaseInfo) : UpdateState()
    data class Downloading(val release: ReleaseInfo, val percent: Int) : UpdateState()
    data class Failed(val release: ReleaseInfo?, val message: String) : UpdateState()
}
