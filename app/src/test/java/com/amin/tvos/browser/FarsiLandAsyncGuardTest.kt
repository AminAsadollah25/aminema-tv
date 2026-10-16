package com.amin.tvos.browser

import org.junit.Assert.*
import org.junit.Test

class FarsiLandAsyncGuardTest {
    @Test fun expiredReplyCannotCompleteNewInspection() {
        val guard = FarsiLandAsyncGuard()
        val old = guard.beginInspection()
        guard.expireInspection()
        val fresh = guard.beginInspection()
        assertFalse(guard.currentInspection(old))
        guard.finishInspection(old)
        assertTrue(guard.currentInspection(fresh))
    }
    @Test fun oldTimeoutCannotFailNextRecovery() {
        val guard = FarsiLandAsyncGuard()
        val old = guard.beginRecovery()
        guard.finishRecovery(old)
        val fresh = guard.beginRecovery()
        assertFalse(guard.currentRecovery(old))
        assertTrue(guard.currentRecovery(fresh))
    }
    @Test fun navigationRetryOrPauseInvalidatesAllCallbacks() {
        val guard = FarsiLandAsyncGuard()
        val recovery = guard.beginRecovery()
        val inspection = guard.beginInspection()
        guard.invalidate()
        assertFalse(guard.currentInspection(inspection))
        assertFalse(guard.currentRecovery(recovery))
        assertTrue(guard.currentInspection(guard.beginInspection()))
    }
    @Test fun recoveryInvalidatesPendingInspection() {
        val guard = FarsiLandAsyncGuard()
        val inspection = guard.beginInspection()
        val recovery = guard.beginRecovery()
        assertFalse(guard.currentInspection(inspection))
        assertTrue(guard.currentRecovery(recovery))
    }
    @Test fun completedReplyCannotBeAppliedTwice() {
        val guard = FarsiLandAsyncGuard()
        val inspection = guard.beginInspection()
        guard.finishInspection(inspection)
        assertFalse(guard.currentInspection(inspection))
    }
}
