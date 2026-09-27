package org.futo.voiceinput.shared.util

import android.content.Context
import android.content.Intent

/** Opens the activity where the user can grant the microphone permission to the keyboard. */
fun openMicPermissionSettings(context: Context) {
    val micPermissionRequester = Intent()
    micPermissionRequester.setClassName(context, "org.futo.inputmethod.latin.MicPermissionActivity")
    micPermissionRequester.setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    )
    context.startActivity(micPermissionRequester)
}
