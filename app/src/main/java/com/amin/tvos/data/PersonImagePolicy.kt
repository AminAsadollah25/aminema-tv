package com.amin.tvos.data

import java.net.URI

object PersonImagePolicy {
    fun safeUrl(value: String): String = runCatching {
        val raw = value.trim().takeIf { it.length <= 2000 } ?: return ""
        val uri = URI(raw)
        raw.takeIf { uri.scheme in setOf("http", "https") &&
            !uri.host.isNullOrBlank() && uri.userInfo == null }.orEmpty()
    }.getOrDefault("")
}
