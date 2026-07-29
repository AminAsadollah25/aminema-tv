package com.amin.tvos.data

import java.net.URI

/**
 * Shared trust policy for normal title-page metadata.
 *
 * WebView callbacks are asynchronous, so URL identity and generic shell-title rejection
 * are kept in one tested place for Browser capture, persistence repair and Home display.
 */
object ContentMetadataPolicy {

    fun canonicalContentUrl(value: String): String = runCatching {
        val uri = URI(value.trim())
        val path = uri.path.orEmpty().trimEnd('/').ifBlank { "/" }
        "${uri.scheme?.lowercase()}://${uri.host?.lowercase()}$path"
    }.getOrElse {
        value.substringBefore('#').substringBefore('?').trimEnd('/')
    }

    fun isSameTopLevelPage(first: String, second: String): Boolean =
        first.isNotBlank() &&
            second.isNotBlank() &&
            canonicalContentUrl(first) == canonicalContentUrl(second)

    fun isGenericShellTitle(
        title: String,
        serviceId: String,
        serviceName: String = ""
    ): Boolean {
        val normalized = title.replace(Regex("""\s+"""), " ").trim()
        if (normalized.isBlank() || normalized.equals(serviceName, true)) return true
        return when (serviceId) {
            "parsiflix" ->
                normalized.contains("Parsiflix", ignoreCase = true) &&
                    (
                        normalized.contains("Watch Persian", ignoreCase = true) ||
                            normalized.length < 24
                    )
            "filmrooz" -> normalized.equals("فیلم خارجی", ignoreCase = true)
            else -> false
        }
    }
}
