package com.amin.tvos.data

import com.amin.tvos.data.model.LiveChannel
import com.amin.tvos.data.model.LiveChannelHealth
import com.amin.tvos.data.model.LiveHealthStatus
import com.amin.tvos.data.model.StreamingService

/** A channel together with the provider page that owns it. */
data class LiveChannelSource(
    val service: StreamingService,
    val channel: LiveChannel
)

fun liveChannelKey(source: LiveChannelSource): String =
    "${source.service.id}:${source.channel.id}"

fun liveChannelSources(services: List<StreamingService>): List<LiveChannelSource> =
    services.flatMap { service ->
        service.liveTv?.channels.orEmpty().map { channel ->
            LiveChannelSource(service, channel)
        }
    }

/**
 * Keeps one provider page for a title that exists in more than one directory.
 * A confirmed active page always wins; otherwise the provider order remains stable.
 */
fun deduplicateLiveChannels(
    sources: List<LiveChannelSource>,
    health: Map<String, LiveChannelHealth>
): List<LiveChannelSource> {
    return sources
        .withIndex()
        .groupBy { (_, source) -> normalizeLiveName(source.channel.name).ifBlank { liveChannelKey(source) } }
        .values
        .map { group ->
            group.maxWithOrNull(
                compareBy<IndexedValue<LiveChannelSource>>(
                    { healthRank(health[liveChannelKey(it.value)]?.status) },
                    { providerRank(it.value.service.id) },
                    { -it.index }
                )
            )!!.value
        }
        .sortedBy { source -> sources.indexOfFirst { it == source } }
}

fun normalizeLiveName(value: String): String = value
    .trim()
    .lowercase()
    .replace('ي', 'ی')
    .replace('ك', 'ک')
    .replace(Regex("[\\s\\u200c\\-_–—:؛،,./]+"), "")

fun isLiveActive(
    source: LiveChannelSource,
    health: Map<String, LiveChannelHealth>
): Boolean = health[liveChannelKey(source)]?.status == LiveHealthStatus.ACTIVE

private fun healthRank(status: LiveHealthStatus?): Int = when (status) {
    LiveHealthStatus.ACTIVE -> 3
    LiveHealthStatus.UNKNOWN, null -> 2
    LiveHealthStatus.INACTIVE -> 1
}

private fun providerRank(id: String): Int = when (id) {
    "parsiflix" -> 3
    "parsatv" -> 2
    "babaktv" -> 1
    else -> 0
}
