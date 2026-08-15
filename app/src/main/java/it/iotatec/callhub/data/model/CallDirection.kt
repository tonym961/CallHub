package it.iotatec.callhub.data.model

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED,
    /** VoIP call detected but direction could not be determined from the notification. */
    UNKNOWN
}
