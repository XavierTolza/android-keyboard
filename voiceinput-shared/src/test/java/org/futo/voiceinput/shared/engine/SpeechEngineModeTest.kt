package org.futo.voiceinput.shared.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechEngineModeTest {
    @Test
    fun parsesKnownValuesIgnoringCaseAndWhitespace() {
        assertEquals(SpeechEngineMode.AUTO, SpeechEngineMode.fromStorageString("auto"))
        assertEquals(SpeechEngineMode.ONLINE, SpeechEngineMode.fromStorageString("online"))
        assertEquals(SpeechEngineMode.OFFLINE, SpeechEngineMode.fromStorageString("offline"))
        assertEquals(SpeechEngineMode.OFFLINE, SpeechEngineMode.fromStorageString("local"))
        assertEquals(SpeechEngineMode.ONLINE, SpeechEngineMode.fromStorageString(" ONLINE "))
    }

    @Test
    fun unknownOrMissingValuesFallBackToAuto() {
        assertEquals(SpeechEngineMode.AUTO, SpeechEngineMode.fromStorageString(null))
        assertEquals(SpeechEngineMode.AUTO, SpeechEngineMode.fromStorageString(""))
        assertEquals(SpeechEngineMode.AUTO, SpeechEngineMode.fromStorageString("banana"))
    }

    @Test
    fun storageStringsRoundTrip() {
        for (mode in SpeechEngineMode.entries) {
            assertEquals(mode, SpeechEngineMode.fromStorageString(mode.toStorageString()))
        }
    }
}
