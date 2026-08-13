package com.amin.tvos.update

/** Compares GitHub releases without mixing semantic versions with Android version codes. */
internal object ReleaseVersionPolicy {

    fun isNewer(
        releaseTag: String,
        explicitVersionCode: Int?,
        currentVersionCode: Int,
        currentVersionName: String
    ): Boolean {
        if (explicitVersionCode != null) {
            return explicitVersionCode > currentVersionCode
        }

        val releaseVersion = parseSemanticVersion(releaseTag) ?: return false
        val currentVersion = parseSemanticVersion(currentVersionName) ?: return false
        return releaseVersion > currentVersion
    }

    private fun parseSemanticVersion(value: String): SemanticVersion? {
        val match = VERSION_PATTERN.matchEntire(value.trim()) ?: return null
        return SemanticVersion(
            major = match.groupValues[1].toIntOrNull() ?: return null,
            minor = match.groupValues[2].toIntOrNull() ?: return null,
            patch = match.groupValues[3].ifBlank { "0" }.toIntOrNull() ?: return null
        )
    }

    private data class SemanticVersion(
        val major: Int,
        val minor: Int,
        val patch: Int
    ) : Comparable<SemanticVersion> {
        override fun compareTo(other: SemanticVersion): Int =
            compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor,
                SemanticVersion::patch)
    }

    private val VERSION_PATTERN = Regex("""^[vV]?(\d+)\.(\d+)(?:\.(\d+))?$""")
}
