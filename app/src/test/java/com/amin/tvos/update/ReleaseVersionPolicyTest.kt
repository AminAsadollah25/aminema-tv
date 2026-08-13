package com.amin.tvos.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionPolicyTest {

    @Test
    fun sameSemanticVersionWithoutPublishedCodeIsNotAnUpdate() {
        assertFalse(
            ReleaseVersionPolicy.isNewer(
                releaseTag = "v0.18.2",
                explicitVersionCode = null,
                currentVersionCode = 44,
                currentVersionName = "0.18.2"
            )
        )
    }

    @Test
    fun newerSemanticVersionWithoutPublishedCodeIsAnUpdate() {
        assertTrue(
            ReleaseVersionPolicy.isNewer(
                releaseTag = "v0.18.3",
                explicitVersionCode = null,
                currentVersionCode = 44,
                currentVersionName = "0.18.2"
            )
        )
    }

    @Test
    fun explicitAndroidVersionCodeRemainsThePreferredSourceOfTruth() {
        assertTrue(
            ReleaseVersionPolicy.isNewer(
                releaseTag = "v0.18.2",
                explicitVersionCode = 45,
                currentVersionCode = 44,
                currentVersionName = "0.18.2"
            )
        )
        assertFalse(
            ReleaseVersionPolicy.isNewer(
                releaseTag = "v9.0.0",
                explicitVersionCode = 44,
                currentVersionCode = 44,
                currentVersionName = "0.18.2"
            )
        )
    }

    @Test
    fun malformedFallbackVersionFailsClosed() {
        assertFalse(
            ReleaseVersionPolicy.isNewer(
                releaseTag = "latest",
                explicitVersionCode = null,
                currentVersionCode = 44,
                currentVersionName = "0.18.2"
            )
        )
    }
}
