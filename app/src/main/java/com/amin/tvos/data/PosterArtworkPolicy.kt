package com.amin.tvos.data

/** Accept only a loaded portrait from the verified title's own poster container. */
object PosterArtworkPolicy {
    fun verifiedPortrait(url: String, width: Int, height: Int): String {
        val safe = PersonImagePolicy.safeUrl(url)
        val ratio = if (height > 0) width.toDouble() / height else 0.0
        return safe.takeIf { width >= 300 && height >= 450 && ratio in 0.55..0.8 }.orEmpty()
    }
}
