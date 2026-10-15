package com.amin.tvos.ui.home

import androidx.annotation.DrawableRes
import com.amin.tvos.R

/** Explicit local artwork: no URL lookup, no network and no image work at startup. */
internal object MarathonArtwork {
    val resources: Map<String, Int> = mapOf(
        "middleearth" to R.drawable.marathon_middleearth,
        "starwars" to R.drawable.marathon_starwars,
        "dc" to R.drawable.marathon_dc,
        "potter" to R.drawable.marathon_potter,
        "bond" to R.drawable.marathon_bond,
        "mission" to R.drawable.marathon_mission,
        "wick" to R.drawable.marathon_wick,
        "diehard" to R.drawable.marathon_diehard,
        "bourne" to R.drawable.marathon_bourne,
        "fast" to R.drawable.marathon_fast,
        "rocky" to R.drawable.marathon_rocky,
        "madmax" to R.drawable.marathon_madmax
    )
    @DrawableRes fun forCollection(id: String): Int? = resources[id]
}
