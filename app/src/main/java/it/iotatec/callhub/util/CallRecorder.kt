package it.iotatec.callhub.util

import android.content.Context
import it.iotatec.callhub.sip.SipRegistry
import java.io.File

/**
 * Records the current SIP call via the Linphone engine. Cellular-call recording
 * is intentionally absent — it is not possible for a normal app on modern Android.
 * Recordings are written to the app's external files dir (WAV).
 */
object CallRecorder {

    fun canRecord(): Boolean = SipRegistry.engine.hasActiveCall

    fun isRecording(): Boolean = SipRegistry.engine.isRecording

    /** Toggle recording; returns the new recording state. */
    fun toggle(context: Context): Boolean {
        val engine = SipRegistry.engine
        if (!engine.hasActiveCall) return false
        if (engine.isRecording) {
            engine.stopRecording()
        } else {
            val dir = context.getExternalFilesDir("recordings") ?: context.filesDir
            val file = File(dir, "sip-${System.currentTimeMillis()}.wav")
            engine.startRecording(file.absolutePath)
        }
        return engine.isRecording
    }
}
