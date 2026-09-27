package org.futo.voiceinput.shared.util

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

/** Temporarily takes audio focus while a voice session is active, so videos or music pause. */
class AudioFocusController(
    private val context: Context,
    private val enabled: Boolean
) {
    private var focusRequest: AudioFocusRequest? = null

    fun focus() {
        unfocus()

        if (!enabled) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                focusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .build()
                audioManager.requestAudioFocus(focusRequest!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unfocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                focusRequest = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
