package org.futo.voiceinput.shared.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechEngineSelectionTest {
    @Test
    fun autoUsesOnlineWhenConnectedAndLocalOtherwise() {
        assertEquals(
            SpeechEngineKind.ONLINE,
            selectSpeechEngine(SpeechEngineMode.AUTO, isOnlineUsable = true, canFallBackToLocal = true).kind
        )
        assertEquals(
            SpeechEngineKind.LOCAL,
            selectSpeechEngine(SpeechEngineMode.AUTO, isOnlineUsable = false, canFallBackToLocal = true).kind
        )
    }

    @Test
    fun onlineEngineHasNoFallbackWhenLocalIsUnavailable() {
        // Without an on-device model there is nothing to fall back to
        val selection = selectSpeechEngine(SpeechEngineMode.AUTO, isOnlineUsable = true, canFallBackToLocal = false)
        assertEquals(SpeechEngineKind.ONLINE, selection.kind)
        assertFalse(selection.allowLocalFallback)
    }

    @Test
    fun onlineModeTriesOnlineEvenWhenConnectionCheckFails() {
        val selection = selectSpeechEngine(SpeechEngineMode.ONLINE, isOnlineUsable = false, canFallBackToLocal = true)
        assertEquals(SpeechEngineKind.ONLINE, selection.kind)
        assertTrue(selection.allowLocalFallback)
    }

    @Test
    fun offlineModeNeverUsesOnline() {
        val selection = selectSpeechEngine(SpeechEngineMode.OFFLINE, isOnlineUsable = true, canFallBackToLocal = true)
        assertEquals(SpeechEngineKind.LOCAL, selection.kind)
        assertFalse(selection.allowLocalFallback)
    }

    @Test
    fun fallsBackToLocalOnlyForResumableOnlineFailures() {
        val selection = selectSpeechEngine(SpeechEngineMode.AUTO, isOnlineUsable = true, canFallBackToLocal = true)

        assertTrue(shouldFallBackToLocal(SpeechEngineFailure.NETWORK, selection))
        assertTrue(shouldFallBackToLocal(SpeechEngineFailure.SERVICE, selection))
        assertTrue(shouldFallBackToLocal(SpeechEngineFailure.AUDIO, selection))
        assertTrue(shouldFallBackToLocal(SpeechEngineFailure.OTHER, selection))

        // Nothing was heard: retrying on-device would not help
        assertFalse(shouldFallBackToLocal(SpeechEngineFailure.NO_MATCH, selection))
        // The user must grant the permission first
        assertFalse(shouldFallBackToLocal(SpeechEngineFailure.PERMISSION, selection))
    }

    @Test
    fun neverFallsBackWhenLocalIsUnavailable() {
        val selection = selectSpeechEngine(SpeechEngineMode.AUTO, isOnlineUsable = true, canFallBackToLocal = false)
        assertFalse(shouldFallBackToLocal(SpeechEngineFailure.NETWORK, selection))
    }

    @Test
    fun neverFallsBackWhenAlreadyOnLocalEngine() {
        val selection = selectSpeechEngine(SpeechEngineMode.OFFLINE, isOnlineUsable = true, canFallBackToLocal = true)
        assertFalse(shouldFallBackToLocal(SpeechEngineFailure.NETWORK, selection))
    }

    @Test
    fun onlineModeWithoutLocalModelDoesNotFallBack() {
        val selection = selectSpeechEngine(SpeechEngineMode.ONLINE, isOnlineUsable = false, canFallBackToLocal = false)
        assertEquals(SpeechEngineKind.ONLINE, selection.kind)
        assertFalse(selection.allowLocalFallback)
        assertFalse(shouldFallBackToLocal(SpeechEngineFailure.NETWORK, selection))
    }

    @Test
    fun localSelectionNeverAllowsFallback() {
        // Falling back to a local engine makes no sense when local is already selected
        for (mode in SpeechEngineMode.entries) {
            for (isOnlineUsable in listOf(true, false)) {
                for (canFallBackToLocal in listOf(true, false)) {
                    val selection = selectSpeechEngine(mode, isOnlineUsable, canFallBackToLocal)
                    if (selection.kind == SpeechEngineKind.LOCAL) {
                        assertFalse("fallback set for $mode/$isOnlineUsable/$canFallBackToLocal",
                            selection.allowLocalFallback)
                    }
                }
            }
        }
    }
}
