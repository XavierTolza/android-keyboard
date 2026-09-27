package org.futo.voiceinput.shared.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class OnlineEngineMappingTest {
    @Test
    fun mapsNetworkErrors() {
        for (code in listOf(
            OnlineEngineErrorCodes.ERROR_NETWORK,
            OnlineEngineErrorCodes.ERROR_NETWORK_TIMEOUT,
            OnlineEngineErrorCodes.ERROR_SERVER,
            OnlineEngineErrorCodes.ERROR_SERVER_DISCONNECTED,
            OnlineEngineErrorCodes.ERROR_TOO_MANY_REQUESTS
        )) {
            assertEquals(SpeechEngineFailure.NETWORK, speechEngineFailureFromErrorCode(code))
        }
    }

    @Test
    fun mapsServiceErrors() {
        for (code in listOf(
            OnlineEngineErrorCodes.ERROR_CLIENT,
            OnlineEngineErrorCodes.ERROR_RECOGNIZER_BUSY,
            OnlineEngineErrorCodes.ERROR_LANGUAGE_NOT_SUPPORTED,
            OnlineEngineErrorCodes.ERROR_LANGUAGE_UNAVAILABLE,
            OnlineEngineErrorCodes.ERROR_CANNOT_CHECK_SUPPORT,
            OnlineEngineErrorCodes.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS
        )) {
            assertEquals(SpeechEngineFailure.SERVICE, speechEngineFailureFromErrorCode(code))
        }
    }

    @Test
    fun mapsNoMatchAndTimeoutAsNoMatch() {
        assertEquals(SpeechEngineFailure.NO_MATCH, speechEngineFailureFromErrorCode(OnlineEngineErrorCodes.ERROR_NO_MATCH))
        assertEquals(SpeechEngineFailure.NO_MATCH, speechEngineFailureFromErrorCode(OnlineEngineErrorCodes.ERROR_SPEECH_TIMEOUT))
    }

    @Test
    fun mapsAudioAndPermissionErrors() {
        assertEquals(SpeechEngineFailure.AUDIO, speechEngineFailureFromErrorCode(OnlineEngineErrorCodes.ERROR_AUDIO))
        assertEquals(SpeechEngineFailure.PERMISSION, speechEngineFailureFromErrorCode(OnlineEngineErrorCodes.ERROR_INSUFFICIENT_PERMISSIONS))
    }

    @Test
    fun mapsUnknownCodesToOther() {
        assertEquals(SpeechEngineFailure.OTHER, speechEngineFailureFromErrorCode(0))
        assertEquals(SpeechEngineFailure.OTHER, speechEngineFailureFromErrorCode(-99))
    }

    @Test
    fun mapsRmsDbToBoundedMagnitude() {
        assertEquals(0f, magnitudeFromRmsDb(-2f), 0.0001f)
        assertEquals(0f, magnitudeFromRmsDb(-50f), 0.0001f)
        assertEquals(1f, magnitudeFromRmsDb(10f), 0.0001f)
        assertEquals(1f, magnitudeFromRmsDb(100f), 0.0001f)
        assertEquals(0.5f, magnitudeFromRmsDb(4f), 0.0001f)
    }
}
