package network.bisq.mobile.node.settings.backup.domain

import java.text.Normalizer

/**
 * The backup holds the node's identity keys and travels through the share sheet, so assume it can be
 * attacked offline without limit. Length is what protects it there; composition rules are left out on
 * purpose, as they push people towards predictable patterns. Lengths count code points, not UTF-16 units.
 *
 * The password is normalized before the rules apply and before encrypting. NFKC makes the same visible
 * password the same code points on any keyboard (precomposed or combining accents, full-width forms from
 * CJK keyboards), so a backup restores on another device. Surrounding whitespace is dropped after that: a
 * keyboard can add a trailing space to both password fields alike, and the masked field would never show it.
 */
object BackupPasswordPolicy {
    const val MIN_LENGTH = 12
    const val MAX_LENGTH = 256
    const val MIN_DISTINCT_CHARACTERS = 5

    enum class Violation {
        TOO_SHORT,
        TOO_LONG,
        TOO_FEW_DISTINCT_CHARACTERS,
    }

    fun normalize(password: String): String = Normalizer.normalize(password, Normalizer.Form.NFKC).trim()

    fun findViolation(password: String): Violation? {
        val normalized = normalize(password)
        val length = normalized.codePointCount(0, normalized.length)
        return when {
            length < MIN_LENGTH -> Violation.TOO_SHORT
            length > MAX_LENGTH -> Violation.TOO_LONG
            normalized.codePoints().distinct().count() < MIN_DISTINCT_CHARACTERS -> Violation.TOO_FEW_DISTINCT_CHARACTERS
            else -> null
        }
    }
}
