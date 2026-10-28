package com.amin.tvos.browser

import kotlin.math.abs

/** Existing poll only. A paused/background/unknown page never counts as a stalled movie. */
internal class FarsiLandProgressGuard(private val timeoutMs:Long=20_000L) {
    private var anchor:Double?=null
    private var advancedAt=0L
    fun reset(){anchor=null;advancedAt=0L}
    fun stalled(phase:String,time:Double,paused:Boolean,now:Long):Boolean {
        if(paused || phase !in setOf("playing","buffering") || !time.isFinite() || time<0){reset();return false}
        val previous=anchor
        if(previous==null || abs(time-previous)>=0.5){anchor=time;advancedAt=now;return false}
        return now-advancedAt>=timeoutMs
    }
}
