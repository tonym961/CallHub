package it.iotatec.callhub.transcription

/**
 * Speech-to-text for recorded (SIP) calls. Kept engine-agnostic like [SipEngine]:
 * plug in a real ASR backend and set it via [TranscriptionRegistry.engine].
 *
 * Recommended offline engine: **Vosk** (Apache-2.0) — bundle `com.alphacephei:vosk-android`
 * plus a language model, and implement [transcribe] by feeding the WAV through a
 * `Recognizer`. Alternatively a cloud STT API (needs an API key + INTERNET).
 *
 * Android's platform `SpeechRecognizer` only transcribes live mic input, not files,
 * so it cannot be used to transcribe a saved recording.
 */
interface TranscriptionEngine {
    /** Transcribe the audio file at [audioFilePath]; returns null if unavailable. */
    suspend fun transcribe(audioFilePath: String): String?
}

/** Default: no transcription (returns null). Replace with a Vosk/cloud-backed engine. */
object StubTranscriptionEngine : TranscriptionEngine {
    override suspend fun transcribe(audioFilePath: String): String? = null
}

/** Holds the active transcription engine (swap in a real ASR here). */
object TranscriptionRegistry {
    @Volatile
    var engine: TranscriptionEngine = StubTranscriptionEngine
}
