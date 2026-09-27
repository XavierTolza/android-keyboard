package org.futo.voiceinput.shared.engine

/**
 * Error codes as defined by [android.speech.SpeechRecognizer], kept as plain
 * constants so that this mapping stays testable on the JVM.
 */
object OnlineEngineErrorCodes {
    const val ERROR_NETWORK_TIMEOUT = -1
    const val ERROR_NETWORK = -2
    const val ERROR_AUDIO = -3
    const val ERROR_SERVER = -4
    const val ERROR_CLIENT = -5
    const val ERROR_SPEECH_TIMEOUT = -6
    const val ERROR_NO_MATCH = -7
    const val ERROR_RECOGNIZER_BUSY = -8
    const val ERROR_INSUFFICIENT_PERMISSIONS = -9
    const val ERROR_SERVER_DISCONNECTED = -10
    const val ERROR_TOO_MANY_REQUESTS = -11
    const val ERROR_LANGUAGE_NOT_SUPPORTED = -12
    const val ERROR_LANGUAGE_UNAVAILABLE = -13
    const val ERROR_CANNOT_CHECK_SUPPORT = -14
    const val ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS = -15
}

/** Translates an online recognition error code into a [SpeechEngineFailure]. */
fun speechEngineFailureFromErrorCode(code: Int): SpeechEngineFailure {
    return when (code) {
        OnlineEngineErrorCodes.ERROR_NETWORK,
        OnlineEngineErrorCodes.ERROR_NETWORK_TIMEOUT,
        OnlineEngineErrorCodes.ERROR_SERVER,
        OnlineEngineErrorCodes.ERROR_SERVER_DISCONNECTED,
        OnlineEngineErrorCodes.ERROR_TOO_MANY_REQUESTS -> SpeechEngineFailure.NETWORK

        OnlineEngineErrorCodes.ERROR_CLIENT,
        OnlineEngineErrorCodes.ERROR_RECOGNIZER_BUSY,
        OnlineEngineErrorCodes.ERROR_LANGUAGE_NOT_SUPPORTED,
        OnlineEngineErrorCodes.ERROR_LANGUAGE_UNAVAILABLE,
        OnlineEngineErrorCodes.ERROR_CANNOT_CHECK_SUPPORT,
        OnlineEngineErrorCodes.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> SpeechEngineFailure.SERVICE

        OnlineEngineErrorCodes.ERROR_SPEECH_TIMEOUT,
        OnlineEngineErrorCodes.ERROR_NO_MATCH -> SpeechEngineFailure.NO_MATCH

        OnlineEngineErrorCodes.ERROR_AUDIO -> SpeechEngineFailure.AUDIO

        OnlineEngineErrorCodes.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechEngineFailure.PERMISSION

        else -> SpeechEngineFailure.OTHER
    }
}

/**
 * Maps the RMS value reported by online recognition (roughly -2..10 dB) to the
 * 0..1 bubble magnitude used by the recording UI.
 */
fun magnitudeFromRmsDb(rmsDb: Float): Float {
    return ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
}
