package network.bisq.mobile.domain.analytics

/**
 * Defense-in-depth PII scrubber for analytics payloads.
 *
 * The sealed [AnalyticsEvent] hierarchy is the primary guarantee that custom
 * events carry no PII (no free-form props). This redactor exists for the one
 * payload we cannot statically constrain — exception messages and stack-trace
 * strings, which carry whatever the JVM or coroutine internals happen to
 * compose at runtime (file paths from the filesystem, error messages that may
 * quote user input, IP addresses from network failures, etc.).
 *
 * Pattern coverage matches the agreement on bisq-network/bisq-mobile#525:
 * emails, BTC addresses (legacy, P2SH, bech32, taproot), onion v3 hostnames,
 * seed-phrase shapes, file paths under /Users/, /home/, C:\Users\, IPv4, IPv6.
 * The expressions themselves are defined once in [RedactionPatterns].
 *
 * Each pattern is replaced with a token (`<email>`, `<btc_address>`, …) rather
 * than empty string so a developer reading a redacted event can see what
 * *kind* of secret was scrubbed.
 *
 * Ordering matters: most-specific patterns first so they consume their input
 * before less-specific patterns get a chance to false-positive on overlapping
 * substrings. Seed-phrase regex runs LAST because it would otherwise greedily
 * match lowercase fragments of already-redacted content.
 */
class AnalyticsRedactor {
    fun redact(input: String): String {
        if (input.isEmpty()) return input
        var s = input
        for ((pattern, token) in PATTERNS) {
            s = pattern.replace(s, token)
        }
        return s
    }

    private companion object {
        // Tokens are angle-bracketed to keep them out of the seed-phrase
        // lowercase-words matcher that runs at the end of the pipeline.
        private const val EMAIL_TOKEN = "<email>"
        private const val BTC_TOKEN = "<btc_address>"
        private const val ONION_TOKEN = "<onion>"
        private const val PATH_TOKEN = "<path>"
        private const val IP_TOKEN = "<ip>"
        private const val SEED_TOKEN = "<seed_phrase>"

        // The expressions live in RedactionPatterns, shared with the log scrubber so the two
        // redaction paths cannot drift apart. Only the token mapping and order are decided here.
        private val ONION = RedactionPatterns.ONION
        private val EMAIL = RedactionPatterns.EMAIL
        private val BTC_BECH32 = RedactionPatterns.BTC_BECH32
        private val BTC_BASE58 = RedactionPatterns.BTC_BASE58
        private val IPV4 = RedactionPatterns.IPV4
        private val IPV6 = RedactionPatterns.IPV6
        private val FILE_PATH = RedactionPatterns.FILE_PATH
        private val SEED_PHRASE = RedactionPatterns.SEED_PHRASE

        // Order: specific → general. Seed-phrase last.
        private val PATTERNS =
            listOf(
                ONION to ONION_TOKEN,
                EMAIL to EMAIL_TOKEN,
                BTC_BECH32 to BTC_TOKEN,
                BTC_BASE58 to BTC_TOKEN,
                IPV6 to IP_TOKEN,
                IPV4 to IP_TOKEN,
                FILE_PATH to PATH_TOKEN,
                SEED_PHRASE to SEED_TOKEN,
            )
    }
}
