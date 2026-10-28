package com.amin.tvos.browser

/** A first decoded frame is not proof of sustained playback. No wall-clock timers or I/O. */
internal class FarsiLandStartupGate {
    var confirmed = false
        private set
    private var progressingSince: Long? = null
    private var firstTime = 0.0

    fun reset() { confirmed=false;progressingSince=null;firstTime=0.0 }

    fun observe(phase:String,time:Double,now:Long,userPaused:Boolean=false):Boolean {
        if(confirmed)return true
        // An explicit user Pause must remain visible/usable, never trigger startup recovery.
        if(userPaused && phase=="paused"){confirmed=true;return true}
        if(phase!="playing" || !time.isFinite() || time<=0){progressingSince=null;return false}
        val since=progressingSince
        if(since==null || time<firstTime){progressingSince=now;firstTime=time;return false}
        if(now-since>=2000L && time-firstTime>=1.0)confirmed=true
        return confirmed
    }
}
