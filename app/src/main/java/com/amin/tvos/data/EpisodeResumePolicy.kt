package com.amin.tvos.data

/** Normal DOM selection tokens only; never a media URL. */
object EpisodeResumePolicy {
    private val pattern = Regex("^#(parsiflix|mymoviz)-s([0-9]+)-epnum-([0-9]+)$|^#filmrooz-s([0-9]+)-box([0-9]+)-epnum-([0-9]+)$")
    fun safeAction(value: String): String = value.takeIf { it.length < 100 && pattern.matches(it) }.orEmpty()
    fun season(value: String): String {
        val match = pattern.matchEntire(value) ?: return ""
        return match.groupValues[2].ifBlank { match.groupValues[4] }
    }
    fun episode(value: String): String {
        val match = pattern.matchEntire(value) ?: return ""
        return match.groupValues[3].ifBlank { match.groupValues[6] }
    }
    fun sameEpisode(previousAction: String, nextAction: String, previousPage: String, nextPage: String): Boolean =
        if (previousAction.isNotBlank() || nextAction.isNotBlank()) previousAction == nextAction
        else previousPage == nextPage
}
