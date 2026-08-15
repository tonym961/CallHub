package it.iotatec.callhub.dialer.spam

/**
 * Pluggable phone-number reputation lookup. Implement this to add an online
 * source (a community list, a free-tier API such as tellows, or your own
 * server). Keep lookups fast and side-effect free — [CallScreeningService] runs
 * them on the incoming-call hot path.
 */
interface ReputationProvider {
    enum class Verdict { UNKNOWN, TRUSTED, SPAM }

    /** Called off the main thread. Return quickly; never block indefinitely. */
    suspend fun lookup(number: String): Verdict
}

/**
 * Default: no network calls, always UNKNOWN. Swap this out for a real provider.
 * Example integration point for a free source (pseudo):
 *
 *   class TellowsProvider(private val apiKey: String) : ReputationProvider {
 *       override suspend fun lookup(number: String): Verdict { ... HTTP GET ... }
 *   }
 */
object NoOpReputationProvider : ReputationProvider {
    override suspend fun lookup(number: String): ReputationProvider.Verdict =
        ReputationProvider.Verdict.UNKNOWN
}

/** Central place to swap the active provider. */
object Reputation {
    @Volatile
    var provider: ReputationProvider = NoOpReputationProvider
}
