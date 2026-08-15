package it.iotatec.callhub.sip

/** A SIP/VoIP account. [id] is stable (username@domain) and used as the Telecom account id. */
data class SipAccount(
    val displayName: String,
    val username: String,
    val domain: String,
    val password: String,
    val port: Int = 5060,
    val transport: Transport = Transport.UDP
) {
    val id: String get() = "$username@$domain"

    enum class Transport { UDP, TCP, TLS }
}
