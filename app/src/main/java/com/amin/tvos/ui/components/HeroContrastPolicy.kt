package com.amin.tvos.ui.components

import kotlin.math.pow

object HeroContrastPolicy {
    private fun linear(v: Double): Double = if(v<=0.04045) v/12.92 else ((v+0.055)/1.055).pow(2.4)
    fun luminance(r: Double,g: Double,b: Double) =
        0.2126*linear(r)+0.7152*linear(g)+0.0722*linear(b)
    // Black and white meet at ~0.179; the better choice is at least 4.58:1.
    fun useDarkText(r: Double,g: Double,b: Double) = luminance(r,g,b)>0.179
    fun isBrightArtwork(r: Double,g: Double,b: Double) = luminance(r,g,b)>0.55
}
