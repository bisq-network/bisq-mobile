package network.bisq.mobile.domain.service.trades

import network.bisq.mobile.i18n.i18n

/**
 * Expected protocol-validation rejections from bisq2 (bad/hostile peer
 * messages). Mobile only sees the error string, so we match core-authored
 * prefixes. peersErrorMessage arrives over the wire; a mismatch fails open
 * to captureException.
 */
object ExpectedTradeProtocolRejection {
    // Prefix-only so trailing details (max length, offer dump) still match.
    private val expectedPrefixes =
        listOf(
            "Bitcoin address length must not be longer than",
            "Lightning invoice length must not be longer than",
            "Bitcoin payment data must not be empty",
            "Takers (buyers) Bitcoin amount is too high",
            "Takers (sellers) Bitcoin amount is too low",
            "Could not find matching offer",
            "Mediators do not match",
        )

    fun isExpected(message: String): Boolean = expectedPrefixes.any { message.startsWith(it) }

    /**
     * Desktop Overlay.failure() uses a different headline when the failure is
     * the peer's (`Trade failed at your peer`). Client REST 400s include
     * `at the peers side`; node peersErrorMessage is unmarked, so [markAtPeer]
     * wraps it with the same phrase before it reaches the presenter.
     */
    fun isAtPeer(message: String): Boolean =
        message.contains(PEERS_SIDE_PHRASE, ignoreCase = true) ||
            message.contains(FAILED_AT_PEER_PHRASE, ignoreCase = true)

    fun markAtPeer(message: String): String = if (isAtPeer(message)) message else "$PEERS_SIDE_PREFIX$message"

    /**
     * Pulls the core-authored protocol text out of a take-offer error string.
     * Facades sometimes wrap it (`Failed to take offer: …`, `The trade failed: '…'\n\nStack trace:`);
     * the client REST 400 is `Invalid input: An error occurred at the peers side at taking
     * the offer: … . ErrorStackTrace: …`. The Trade Failed dialog should show the same
     * raw reason desktop does — no stack-trace tail.
     */
    fun extractExpected(message: String): String? {
        val prefix = expectedPrefixes.firstOrNull { message.contains(it) } ?: return null
        var extracted = message.substring(message.indexOf(prefix))
        val stackIdx = STACK_TRACE_MARKERS.map { extracted.indexOf(it) }.filter { it >= 0 }.minOrNull()
        if (stackIdx != null) {
            extracted = extracted.substring(0, stackIdx)
        }
        // Node wraps with quotes. The REST marker is `. ErrorStackTrace:`, so the
        // cut already drops the API's extra period and leaves the core sentence.
        return extracted.trim().trimEnd('\'').trim()
    }

    /**
     * Best user-facing take-offer error from a throwable. Walks the cause chain
     * so a wrapper with a null [Throwable.message] (common for Java futures /
     * FSM errors) still yields the core protocol text when present.
     */
    fun fromThrowable(error: Throwable): String {
        val chain = causeChain(error)
        val atPeer = chain.mapNotNull { it.message }.any { isAtPeer(it) }
        chain.mapNotNull { it.message }.forEach { message ->
            extractExpected(message)?.let { extracted ->
                return if (atPeer) markAtPeer(extracted) else extracted
            }
        }
        if (isTimeout(error)) {
            return "mobile.takeOffer.sendTimedOut".i18n()
        }
        val fallback =
            chain.firstNotNullOfOrNull { it.message?.takeIf { message -> message.isNotBlank() } }
                ?: error.toString()
        return if (atPeer) markAtPeer(fallback) else fallback
    }

    fun isTimeout(error: Throwable): Boolean =
        causeChain(error).any { throwable ->
            val name = throwable::class.simpleName.orEmpty()
            name.contains("TimeoutException") || name.contains("TimeoutCancellation")
        }

    private fun causeChain(error: Throwable): List<Throwable> =
        generateSequence(error) { current ->
            current.cause?.takeIf { it !== current }
        }.take(16).toList()

    private val STACK_TRACE_MARKERS =
        listOf(
            "\n\nStack trace:",
            "\n\nStack-Trace:",
            // TradeRestApi.takeOffer 400: "<msg>. ErrorStackTrace: <500 chars>".
            // Core text already ends with `.`, so the body has `.. ErrorStackTrace:`.
            ". ErrorStackTrace:",
        )

    private const val PEERS_SIDE_PHRASE = "at the peers side"
    private const val PEERS_SIDE_PREFIX = "An error occurred at the peers side at taking the offer: "
    private const val FAILED_AT_PEER_PHRASE = "failed at your peer"
}
