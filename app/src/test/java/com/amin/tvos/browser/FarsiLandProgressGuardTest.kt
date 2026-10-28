package com.amin.tvos.browser

import org.junit.Assert.*
import org.junit.Test

class FarsiLandProgressGuardTest {
    @Test fun playingButFrozenEventuallyStalls(){
        val guard=FarsiLandProgressGuard()
        assertFalse(guard.stalled("buffering",82.0,false,0))
        assertFalse(guard.stalled("buffering",82.0,false,19999))
        assertTrue(guard.stalled("buffering",82.0,false,20000))
    }
    @Test fun manualPauseNeverTriggersRecovery(){
        val guard=FarsiLandProgressGuard()
        guard.stalled("playing",20.0,false,0)
        assertFalse(guard.stalled("paused",20.0,true,30000))
        assertFalse(guard.stalled("playing",20.0,false,60000))
    }
    @Test fun realProgressRenewsTheDeadline(){
        val guard=FarsiLandProgressGuard()
        guard.stalled("playing",1.0,false,0)
        assertFalse(guard.stalled("playing",2.0,false,15000))
        assertFalse(guard.stalled("buffering",2.0,false,30000))
        assertTrue(guard.stalled("buffering",2.0,false,35000))
    }
    @Test fun seekAndLifecycleResetHaveFreshBudgets(){
        val guard=FarsiLandProgressGuard()
        guard.stalled("playing",10.0,false,0)
        assertFalse(guard.stalled("buffering",998.0,false,25000))
        guard.reset()
        assertFalse(guard.stalled("buffering",998.0,false,90000))
    }
    @Test fun unknownPagesAndInvalidTimesAreNeverPlaybackStalls(){
        val guard=FarsiLandProgressGuard()
        assertFalse(guard.stalled("waiting",1.0,false,0))
        assertFalse(guard.stalled("waiting",1.0,false,90000))
        assertFalse(guard.stalled("playing",Double.NaN,false,100000))
        assertFalse(guard.stalled("playing",-1.0,false,120000))
    }
}
