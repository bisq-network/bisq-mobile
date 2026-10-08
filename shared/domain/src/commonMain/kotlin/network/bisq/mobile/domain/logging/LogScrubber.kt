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

    // Set while inside a profile text value that spans lines; the placeholder every continuation
    // line collapses to, until the line that carries the closing quote.
    private var openProfileField: String? = null

    /** Distinct values replaced so far, per class, in first-seen order of the classes. */
    val summary: Map<String, Int>
        get() = registries.mapValues { it.value.size }

    fun scrubLine(line: String): String {
        openProfileField?.let { placeholder ->
            // A new log record ends the span whatever the value looked like: the safety net
            // against a misread opening quote swallowing the rest of the file.
            if (isRecordStart(line)) {
                openProfileField = null
            } else {
                val close = RedactionPatterns.PROFILE_TEXT_FIELD_CLOSE.find(line)
                return if (close == null) {
                    placeholder
                } else {
                    openProfileField = null
                    "$placeholder'${close.groupValues[1]}"
                }
            }
        }
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

    /**
     * Three shapes, tried in order. A field that ends the line (bisq2's multi-line `toString`) is
     * replaced up to the last quote, so an apostrophe inside the value does not cut it short. A
     * compact single-line dump is replaced field by field. Only a field with no closing quote at all
     * after it opens a multi-line span: the rest of this line is replaced and every following line
     * collapses to the same placeholder until [RedactionPatterns.PROFILE_TEXT_FIELD_CLOSE] matches
     * or a new log record starts.
     */
    private fun replaceProfileTextFields(s: String): String {
        for (pattern in listOf(RedactionPatterns.PROFILE_TEXT_FIELD, RedactionPatterns.PROFILE_TEXT_FIELD_INLINE)) {
            // A field that closes on this line is final, even when its value is empty and nothing changes.
            if (pattern.containsMatchIn(s)) {
                return pattern.replace(s) { match ->
                    val field = match.groupValues[1]
                    val value = match.groupValues[2]
                    if (value.isEmpty()) match.value else "$field='${placeholder(field.lowercase(), value)}'"
                }
            }
        }
        val open = RedactionPatterns.PROFILE_TEXT_FIELD_OPEN.find(s) ?: return s
        val field = open.groupValues[1]
        val placeholder = placeholder(field.lowercase(), open.groupValues[2])
        openProfileField = placeholder
        return s.substring(0, open.range.first) + "$field='$placeholder"
    }

    private fun replaceBase64Blobs(s: String): String =
        RedactionPatterns.BASE64_BLOB.replace(s) { match ->
            match.groupValues[1] + placeholder(BLOB, match.groupValues[2])
        }

    companion object {
        /** True for a line that begins a bisq2 log record, as opposed to a continuation of the previous one. */
        fun isRecordStart(line: String): Boolean = RedactionPatterns.LOG_RECORD_START.containsMatchIn(line)

        private const val ONION = "onion"
        private const val ONION_PREFIX = "onion-prefix"
        private const val EMAIL = "email"
        private const val BTC = "btc"
        private const val UUID = "uuid"
        private const val HASH = "hash"
        private const val PROFILE = "profile"
        private const val NYM = "nym"
        private const val IP = "ip"
        private const val BLOB = "blob"
        private const val PATH = "path"
        private const val SEED = "seed"
    }
}
