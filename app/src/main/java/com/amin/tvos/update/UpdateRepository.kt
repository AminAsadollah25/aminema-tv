package com.amin.tvos.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONObject

/**
 * Checks GitHub Releases for a newer build and installs it through the normal Android
 * package installer.
 *
 * There is no separate `update.json` manifest: the GitHub Releases API for the repo already
 * carries everything needed (tag, notes, asset URLs), so that's read directly. The APK asset
 * is expected to keep the naming this project already uses (`*-debug.apk` /
 * `*-debug.apk.sha256`), so the same debug-signed, `com.amin.tvos.debug` build this device
 * already runs is what gets installed — that is what lets this land as an in-place update
 * instead of a second, separately-signed app.
 *
 * Installation is never silent: Android requires the user to approve every package install
 * outside Google Play unless the app is a device owner, which this one deliberately is not.
 * A broken build should never be able to auto-apply itself.
 */
class UpdateRepository(private val context: Context) {

    suspend fun checkForUpdate(currentVersionCode: Int): ReleaseInfo? =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = httpGetText(RELEASES_API_URL) {
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
                val root = JSONObject(json)
                val tagName = root.optString("tag_name")
                val body = root.optString("body")
                val assets = root.optJSONArray("assets") ?: return@runCatching null

                var apkUrl: String? = null
                var apkFileName: String? = null
                var sha256Url: String? = null
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val name = asset.optString("name")
                    val url = asset.optString("browser_download_url")
                    when {
                        name.endsWith(".apk", ignoreCase = true) -> {
                            apkUrl = url
                            apkFileName = name
                        }
                        name.endsWith(".sha256", ignoreCase = true) -> sha256Url = url
                    }
                }
                val resolvedApkUrl = apkUrl ?: return@runCatching null
                val resolvedFileName = apkFileName ?: "aminema-update.apk"

                val versionCode = versionCodeFromBody(body) ?: versionCodeFromTag(tagName)
                ?: return@runCatching null

                if (versionCode <= currentVersionCode) return@runCatching null

                ReleaseInfo(
                    versionName = tagName.removePrefix("v"),
                    versionCode = versionCode,
                    apkUrl = resolvedApkUrl,
                    apkFileName = resolvedFileName,
                    sha256Url = sha256Url,
                    notes = body.trim()
                )
            }.getOrNull()
        }

    /**
     * Downloads the APK, checks it against the published sha256 when one is available, and
     * hands it to the system installer. Returns the downloaded file so the caller can report
     * progress; the file is left in the cache dir for the installer to read via FileProvider
     * and is safe to delete once the install screen has been shown.
     */
    suspend fun download(
        release: ReleaseInfo,
        onProgress: (Int) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, release.apkFileName)
        downloadTo(release.apkUrl, target, onProgress)

        val expectedHash = release.sha256Url?.let { url ->
            runCatching { httpGetText(url) }.getOrNull()
                ?.trim()?.substringBefore(' ')?.lowercase()
        }
        if (!expectedHash.isNullOrBlank()) {
            val actualHash = sha256Of(target)
            if (!actualHash.equals(expectedHash, ignoreCase = true)) {
                target.delete()
                error("Checksum mismatch")
            }
        }
        target
    }

    fun canRequestInstalls(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettingsIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    /** A normal `ACTION_VIEW` install intent — the same one Android shows for any sideloaded APK. */
    fun installIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun downloadTo(url: String, target: File, onProgress: (Int) -> Unit) {
        var connection = openConnection(url)
        // GitHub's asset redirect sometimes takes two hops; HttpURLConnection follows same-
        // protocol redirects on its own, but this bounds it in case that ever changes.
        var redirects = 0
        while (connection.responseCode in 300..399 && redirects < 5) {
            val next = connection.getHeaderField("Location") ?: break
            connection.disconnect()
            connection = openConnection(next)
            redirects++
        }
        check(connection.responseCode == HttpURLConnection.HTTP_OK) {
            "HTTP ${connection.responseCode}"
        }
        val total = connection.contentLengthLong
        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var readTotal = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    readTotal += read
                    if (total > 0) onProgress(((readTotal * 100) / total).toInt())
                }
            }
        }
        connection.disconnect()
    }

    private fun httpGetText(url: String, configure: HttpURLConnection.() -> Unit = {}): String {
        val connection = openConnection(url)
        connection.configure()
        check(connection.responseCode == HttpURLConnection.HTTP_OK) {
            "HTTP ${connection.responseCode}"
        }
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            instanceFollowRedirects = true
        }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Preferred source of truth: an explicit `versionCode: N` line in the release notes. */
    private fun versionCodeFromBody(body: String): Int? =
        Regex("""versionCode:\s*(\d+)""").find(body)?.groupValues?.get(1)?.toIntOrNull()

    /** Fallback for a release published without that line: derive an order from the tag itself. */
    private fun versionCodeFromTag(tag: String): Int? {
        val parts = tag.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size < 2) return null
        val major = parts.getOrElse(0) { 0 }
        val minor = parts.getOrElse(1) { 0 }
        val patch = parts.getOrElse(2) { 0 }
        return major * 10_000 + minor * 100 + patch
    }

    private companion object {
        const val RELEASES_API_URL =
            "https://api.github.com/repos/AminAsadollah25/aminema-tv/releases/latest"
    }
}
