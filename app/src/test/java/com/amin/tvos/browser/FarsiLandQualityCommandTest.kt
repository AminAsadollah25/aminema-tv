package com.amin.tvos.browser

import org.junit.Assert.*
import org.junit.Test

class FarsiLandQualityCommandTest {
    @Test fun commandsAcceptOnlyCharacterizedControlValues(){
        listOf("480","720","1080").forEach { label->
            val command=FarsiLandScripts.selectStalledQuality(label)
            assertTrue(command.contains("e.value==='$label'"))
            assertTrue(command.contains("__aminUserPaused"))
            assertTrue(command.contains("suspended"))
        }
    }
    @Test(expected=IllegalArgumentException::class)
    fun neverCreatesAnAutomatic2160Command(){FarsiLandScripts.selectStalledQuality("2160")}
    @Test(expected=IllegalArgumentException::class)
    fun scriptInputIsNotAnArbitraryLabel(){FarsiLandScripts.selectStalledQuality("1080';bad()")}
}
