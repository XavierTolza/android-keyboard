package org.futo.voiceinput.shared.engine

/**
 * A speech-to-text backend: records speech and delivers the transcribed text
 * through an [org.futo.voiceinput.shared.types.AudioRecognizerListener].
 */
interface SpeechEngine {
    /** Ask for the microphone permission if needed, then start listening. */
    fun start()

    /** Stop listening and transcribe whatever has been recorded so far. */
    fun finish()

    /** Abort the session and notify the listener with [org.futo.voiceinput.shared.types.AudioRecognizerListener.cancelled]. */
    fun cancel()

    /** Release all resources; the engine can be started again afterwards. */
    fun reset()

    /** Open the system settings page where the microphone permission can be granted. */
    fun openPermissionSettings()
}
