package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

/**
 * Represents a distinct edition of a series (e.g. Uncut, Extended, or a specific Dub/Quality bundle)
 * Editions are resolved progressively: Edition -> Language -> Quality.
 */
@Serializable
data class SeriesEdition(
    val id: String,
    val label: String,          // e.g. "بدون سانسور", "BluRay", "پیش‌فرض"
    val language: String,       // e.g. "دوبله", "دوزبانه", "اصلی"
    val resolution: String,     // e.g. "1080p", "720p"
    val isDefault: Boolean = false,
    val seasons: List<Season> = emptyList()
)

@Serializable
data class Season(
    val id: String,
    val name: String,           // e.g. "فصل ۱"
    val episodes: List<Episode> = emptyList()
)

@Serializable
data class Episode(
    val id: String,
    val title: String,          // e.g. "قسمت ۱"
    val actionPayload: String,  // Aminema semantic DOM action; never a media/download URL
    val isAvailableOnline: Boolean, // True if streamable (e.g. .eSbox), false if only download
    val isWatched: Boolean      // True if provider shows checkmark
)
