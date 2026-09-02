package network.bisq.mobile.domain.service.trades

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
}
