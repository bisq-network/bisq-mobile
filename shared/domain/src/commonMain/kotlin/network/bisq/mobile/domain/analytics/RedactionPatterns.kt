package network.bisq.mobile.domain.analytics

/**
 * The regular expressions shared by [AnalyticsRedactor] (fixed tokens, analytics payloads) and
 * `LogScrubber` (numbered placeholders, shared log files). One definition per identifier class so
 * the two cannot drift apart.
 *
 * Word boundaries rather than lookbehind: Kotlin/Native's regex engine does not support every
 * lookbehind form, and these run on iOS too.
 */
internal object RedactionPatterns {
    /** Onion v3: 56 chars of base32 (a-z, 2-7) plus the suffix. The suffix makes it safe to run first. */
    val ONION = Regex("""\b[a-z2-7]{56}\.onion\b""")

    /** RFC 5321-ish; requires a TLD of 2+ letters so a bare "@user" mention does not match. */
    val EMAIL = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")

    /** Bech32 segwit / taproot. The checksum is not validated: over-matching is the wanted failure mode. */
    val BTC_BECH32 = Regex("""\bbc1[ac-hj-np-z02-9]{39,87}\b""")

    /** Legacy P2PKH (1...) or P2SH (3...): 25-34 chars of base58. */
    val BTC_BASE58 = Regex("""\b[13][1-9A-HJ-NP-Za-km-z]{25,34}\b""")

    /** Does not match three-octet version strings such as 0.4.1. */
    val IPV4 = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")

    /** Full form only (8 groups); compressed forms would false-positive on prose like "step 1::". */
    val IPV6 = Regex("""\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\b""")

    /** User-home paths on macOS, Linux and Windows, from the home segment to the next whitespace. */
    val FILE_PATH = Regex("""(?:/(?:Users|home)/[^\s]+|[A-Za-z]:\\Users\\[^\s]+)""")

    /** 12+ BIP-39-shaped lowercase words separated by single spaces. Runs last wherever it is used. */
    val SEED_PHRASE = Regex("""\b(?:[a-z]{3,8} +){11,}[a-z]{3,8}\b""")

    /**
     * bisq2 names connection threads after the first 7 chars of the peer's onion host followed by
     * an ellipsis (`Connection.read-TOR-y7y3o6m…-0`). Group 1 is the prefix.
     */
    val ONION_PREFIX = Regex("""\b([a-z2-7]{7})…""")

    /** Trade, offer, connection and key ids in bisq2 are all UUIDs; the shape cannot tell them apart. */
    val UUID = Regex("""\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\b""")

    /** SHA-256 sized hex: proof-of-work, hashes. */
    val HEX_64 = Regex("""\b[0-9a-fA-F]{64}\b""")

    /** RIPEMD-160 sized hex: bisq2 user profile ids and pubKeyHash values. */
    val HEX_40 = Regex("""\b[0-9a-fA-F]{40}\b""")

    /** bisq2 nyms: adverb-adjective-noun-number, see `NymIdGenerator`. */
    val NYM = Regex("""\b[A-Z][a-z]+-[A-Z][a-z]+-[A-Z][a-z]+-\d{1,3}\b""")

    /**
     * Free-text profile fields as bisq2's `UserProfile.toString()` prints them. Group 1 is the
     * field name, group 2 the quoted value including its quotes.
     */
    val PROFILE_TEXT_FIELD = Regex("""\b(nickName|nickname|userName|statement|terms)=('[^']*'|"[^"]*")""")

    /**
     * Base64 material of 40+ chars (public keys, signatures, hashes). Delimited by a non-base64
     * character on both sides; group 1 keeps the leading delimiter, group 2 is the blob.
     */
    val BASE64_BLOB = Regex("""(^|[^A-Za-z0-9+/])([A-Za-z0-9+/]{40,}={0,2})(?=[^A-Za-z0-9+/]|$)""")
}
