package com.amin.tvos.browser

import org.junit.Assert.*
import org.junit.Test

class FarsiLandStartupGateTest {
    @Test fun firstFrameAndAutomaticPauseAreNotStablePlayback(){
        val gate=FarsiLandStartupGate()
        assertFalse(gate.observe("playing",1.0,0))
        assertFalse(gate.observe("paused",1.0,3000))
        assertFalse(gate.confirmed)
    }
    @Test fun actualProgressOverTwoSecondsConfirmsStartup(){
        val gate=FarsiLandStartupGate()
        assertFalse(gate.observe("playing",1.0,0))
        assertFalse(gate.observe("playing",2.0,1000))
        assertTrue(gate.observe("playing",3.0,2000))
        assertTrue(gate.observe("paused",3.0,4000))
    }
    @Test fun frozenFrameDoesNotPassEvenWhenTimeHasElapsed(){
        val gate=FarsiLandStartupGate()
        assertFalse(gate.observe("playing",1.0,0))
        assertFalse(gate.observe("playing",1.0,12000))
    }
    @Test fun automaticPauseRestartsTheConfirmationWindow(){
        val gate=FarsiLandStartupGate()
        gate.observe("playing",1.0,0);gate.observe("paused",1.0,2000)
        assertFalse(gate.observe("playing",2.0,5000))
        assertFalse(gate.observe("playing",3.0,6000))
        assertTrue(gate.observe("playing",4.0,7000))
    }
    @Test fun deliberatePauseRemainsUsableInsteadOfTimingOut(){
        val gate=FarsiLandStartupGate()
        assertTrue(gate.observe("paused",1.0,0,userPaused=true))
        assertTrue(gate.confirmed)
    }
    @Test fun newDocumentAndInvalidDataCannotInheritConfirmation(){
        val gate=FarsiLandStartupGate()
        gate.observe("playing",1.0,0);gate.observe("playing",3.0,2500)
        gate.reset();assertFalse(gate.confirmed)
        assertFalse(gate.observe("playing",Double.NaN,3000))
        assertFalse(gate.observe("playing",Double.POSITIVE_INFINITY,6000))
        assertFalse(gate.observe("buffering",10.0,9000))
    }
}
