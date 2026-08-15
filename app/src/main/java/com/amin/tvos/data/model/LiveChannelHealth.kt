package com.amin.tvos.data.model

import kotlinx.serialization.Serializable

/** Result of one deliberate live-channel probe. No media URL is retained. */
@Serializable
enum class LiveHealthStatus {
    UNKNOWN,
    ACTIVE,
    INACTIVE
}

@Serializable
data class LiveChannelHealth(
    val status: LiveHealthStatus = LiveHealthStatus.UNKNOWN,
    val checkedAt: Long = 0L
)
