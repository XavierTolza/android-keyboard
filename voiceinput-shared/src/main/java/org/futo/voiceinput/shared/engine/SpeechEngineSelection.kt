package org.futo.voiceinput.shared.engine

/**
 * Chooses which speech-to-text backend converts speech into text.
 *
 * The value is persisted in settings, so the storage strings must stay stable.
 */
enum class SpeechEngineMode {
    /**
     * Use online speech recognition when a connection is available,
     * and the on-device model otherwise.
     */
    AUTO,

    /**
     * Always try online speech recognition first, even if no connection is
     * detected up-front. May fall back to the on-device model on failure.
     */
    ONLINE,

    /** Never use online speech recognition; always transcribe on-device. */
    OFFLINE;

    fun toStorageString(): String = name.lowercase()

    companion object {
        fun fromStorageString(value: String?): SpeechEngineMode {
            return when (value?.trim()?.lowercase()) {
                "online" -> ONLINE
                "offline", "local" -> OFFLINE
                else -> AUTO
            }
        }
    }
}

/** Which backend a speech session is running on. */
enum class SpeechEngineKind {
    /** Android speech recognition (usually Google's online service). */
    ONLINE,

    /** On-device recognition with the bundled/downloaded Whisper models. */
    LOCAL
}

/** Why an online speech recognition session failed. */
enum class SpeechEngineFailure {
    /** No network connection, or the recognition servers could not be reached. */
    NETWORK,

    /** The recognition service itself failed or is missing. */
    SERVICE,

    /** Nothing intelligible was recognized. */
    NO_MATCH,

    /** The microphone delivered no usable audio. */
    AUDIO,

    /** The microphone permission is missing. */
    PERMISSION,

    /** Anything else. */
    OTHER
}

data class SpeechEngineSelection(
    val kind: SpeechEngineKind,
    /** Whether a failure of the selected engine may fall back to the on-device engine. */
    val allowLocalFallback: Boolean
)

/**
 * Decides which engine to use for a speech session.
 *
 * @param isOnlineUsable whether online recognition is possible at all right now
 * (recognition service present and network connected)
 * @param canFallBackToLocal whether a local fallback is available and allowed
 * (on-device model installed and fallback enabled in settings)
 */
fun selectSpeechEngine(
    mode: SpeechEngineMode,
    isOnlineUsable: Boolean,
    canFallBackToLocal: Boolean
): SpeechEngineSelection {
    return when (mode) {
        SpeechEngineMode.OFFLINE ->
            SpeechEngineSelection(SpeechEngineKind.LOCAL, allowLocalFallback = false)

        SpeechEngineMode.ONLINE ->
            // The user explicitly asked for online recognition: try even when the
            // connectivity check is pessimistic, the failure path handles the fallback
            SpeechEngineSelection(SpeechEngineKind.ONLINE, allowLocalFallback = canFallBackToLocal)

        SpeechEngineMode.AUTO ->
            if (isOnlineUsable) {
                SpeechEngineSelection(SpeechEngineKind.ONLINE, allowLocalFallback = canFallBackToLocal)
            } else {
                SpeechEngineSelection(SpeechEngineKind.LOCAL, allowLocalFallback = false)
            }
    }
}

/** Whether the session should be retried with the on-device model after an online [failure]. */
fun shouldFallBackToLocal(failure: SpeechEngineFailure, selection: SpeechEngineSelection): Boolean {
    return selection.kind == SpeechEngineKind.ONLINE &&
            selection.allowLocalFallback &&
            failure != SpeechEngineFailure.NO_MATCH &&
            failure != SpeechEngineFailure.PERMISSION
}
