package network.bisq.mobile.domain.logging

import network.bisq.mobile.domain.analytics.RedactionPatterns

/**
 * Rewrites log text so it can leave the device: every identifier that links a log to a person,
 * a Tor identity or a trade is replaced by a numbered placeholder such as `<onion#3>`.
 *
 * Placeholders are stable within one scrubber instance: the same raw value always maps to the same
 * number, so a maintainer can still follow one peer or one trade across lines, while nothing in
 * the output can be mapped back. Line structure and timestamps are untouched, which keeps the
 * file readable and diffable against the code that wrote it.
 *
 * Classes, in the order they are applied (specific before general, so a more specific match
 * consumes its text before a broader pattern can see it):
 * onion hosts, emails, bitcoin addresses, UUIDs (trade, offer, connection and key ids), 64-hex
 * hashes, 40-hex profile ids, nyms, the free-text profile fields bisq2 prints (nickname, username,
 * statement, terms), IPs, base64 key material, home paths and seed phrases.
 *
 * Use one instance per output: the registry grows with the distinct values seen, which for a
 * 10 MB bisq2 log is a few thousand short strings.
 */
class LogScrubber {
    private val registries = LinkedHashMap<String, LinkedHashMap<String, Int>>()

    /** Distinct values replaced so far, per class, in first-seen order of the classes. */
    val summary: Map<String, Int>
        get() = registries.mapValues { it.value.size }

    fun scrubLine(line: String): String {
        if (line.isEmpty()) return line
        var s = line
        s = replaceAll(s, RedactionPatterns.ONION, ONION)
        s = replaceOnionPrefixes(s)
        s = replaceAll(s, RedactionPatterns.EMAIL, EMAIL)
        s = replaceAll(s, RedactionPatterns.BTC_BECH32, BTC)
        s = replaceAll(s, RedactionPatterns.BTC_BASE58, BTC)
        s = replaceAll(s, RedactionPatterns.UUID, UUID)
        s = replaceAll(s, RedactionPatterns.HEX_64, HASH)
        s = replaceAll(s, RedactionPatterns.HEX_40, PROFILE)
        s = replaceAll(s, RedactionPatterns.NYM, NYM)
        s = replaceProfileTextFields(s)
        s = replaceAll(s, RedactionPatterns.IPV6, IP)
        s = replaceAll(s, RedactionPatterns.IPV4, IP)
        s = replaceBase64Blobs(s)
        s = replaceAll(s, RedactionPatterns.FILE_PATH, PATH)
        s = replaceAll(s, RedactionPatterns.SEED_PHRASE, SEED)
        return s
    }

    /** Scrubs multi-line text line by line; line breaks are preserved as given. */
    fun scrub(text: String): String {
        if (text.isEmpty()) return text
        return text.lineSequence().joinToString("\n") { scrubLine(it) }
    }

    /** One human-readable line for the top or bottom of a shared file, e.g. `309 onion, 71 profile`. */
    fun summaryLine(): String {
        val counts = summary.filterValues { it > 0 }
        if (counts.isEmpty()) return "nothing needed redaction"
        return counts.entries.joinToString(", ") { (cls, n) -> "$n $cls" }
    }

    private fun placeholder(
        cls: String,
        raw: String,
    ): String {
        val registry = registries.getOrPut(cls) { LinkedHashMap() }
        val index = registry.getOrPut(raw) { registry.size + 1 }
        return "<$cls#$index>"
    }

    private fun replaceAll(
        s: String,
        regex: Regex,
        cls: String,
    ): String = regex.replace(s) { match -> placeholder(cls, match.value) }

    /**
     * A truncated onion in a thread name is mapped to the placeholder of the full host when that
     * host was already seen, so the thread can be read together with its connection lines;
     * otherwise it gets a placeholder of its own.
     */
    private fun replaceOnionPrefixes(s: String): String =
        RedactionPatterns.ONION_PREFIX.replace(s) { match ->
            val prefix = match.groupValues[1]
            val known = registries[ONION]?.entries?.firstOrNull { it.key.startsWith(prefix) }
            val replacement = if (known != null) "<$ONION#${known.value}>" else placeholder(ONION_PREFIX, prefix)
            "$replacement\u2026"
        }

    private fun replaceProfileTextFields(s: String): String =
        RedactionPatterns.PROFILE_TEXT_FIELD.replace(s) { match ->
            val field = match.groupValues[1]
            val quoted = match.groupValues[2]
            val value = quoted.substring(1, quoted.length - 1)
            if (value.isEmpty()) {
                match.value
            } else {
                val quote = quoted.first()
                "$field=$quote${placeholder(field.lowercase(), value)}$quote"
            }
        }

    private fun replaceBase64Blobs(s: String): String =
        RedactionPatterns.BASE64_BLOB.replace(s) { match ->
            match.groupValues[1] + placeholder(BLOB, match.groupValues[2])
        }

    private companion object {
        const val ONION = "onion"
        const val ONION_PREFIX = "onion-prefix"
        const val EMAIL = "email"
        const val BTC = "btc"
        const val UUID = "uuid"
        const val HASH = "hash"
        const val PROFILE = "profile"
        const val NYM = "nym"
        const val IP = "ip"
        const val BLOB = "blob"
        const val PATH = "path"
        const val SEED = "seed"
    }
}
