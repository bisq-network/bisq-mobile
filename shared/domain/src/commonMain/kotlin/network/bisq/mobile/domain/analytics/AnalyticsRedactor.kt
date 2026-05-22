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

        // Onion v3: 56 chars of base32 (a-z, 2-7) + .onion suffix. Distinct
        // suffix makes this safe to run first. Matches bare hostnames AND
        // URLs (we redact only the hostname; the surrounding URL scheme/path
        // does not carry PII on its own).
        private val ONION = Regex("""\b[a-z2-7]{56}\.onion\b""")

        // RFC 5321-ish email. Conservative: requires a TLD of 2+ letters so a
        // bare "@user" mention won't false-positive.
        private val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

        // Bech32 segwit / taproot: starts `bc1`, 39-87 chars of bech32
        // alphabet. We don't validate the checksum — over-matching on a
        // bech32-shaped string is the desired failure mode for a redactor.
        private val BTC_BECH32 = Regex("""\bbc1[ac-hj-np-z02-9]{39,87}\b""")

        // Legacy P2PKH (starts with 1) or P2SH (starts with 3): 25-34 chars
        // of base58. Lookbehind would let us anchor more tightly but Kotlin
        // Native regex doesn't support all lookbehind features; word boundary
        // is good enough.
        private val BTC_BASE58 = Regex("""\b[13][1-9A-HJ-NP-Za-km-z]{25,34}\b""")

        // IPv4. Won't match three-octet "0.4.1"-style version strings.
        private val IPV4 = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")

        // IPv6 full form (8 groups). Compressed forms (`::1`) are valid but
        // hard to regex without false-positives on prose like "step 1::".
        // Documenting limit so a future reader knows to extend if needed.
        private val IPV6 = Regex("""\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\b""")

        // User-home filesystem paths. We redact from the home segment to the
        // next whitespace so the username AND any sensitive subpath go.
        //
        // Three OS conventions:
        //   /Users/<name>/...   (macOS)
        //   /home/<name>/...    (Linux)
        //   C:\Users\<name>\... (Windows)
        //
        // System paths like /etc/hosts or /usr/lib never start with these
        // prefixes so they remain intact.
        private val FILE_PATH = Regex("""(?:/(?:Users|home)/[^\s]+|[A-Za-z]:\\Users\\[^\s]+)""")

        // Seed-phrase shape: 12+ consecutive BIP-39-style words (4-8 lowercase
        // letters) separated by single spaces. Conservative enough to skip
        // ordinary prose (which has mixed word lengths and punctuation) but
        // catches the dump-the-mnemonic-into-a-log shape. Runs LAST.
        private val SEED_PHRASE = Regex("""\b(?:[a-z]{3,8} +){11,}[a-z]{3,8}\b""")

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
