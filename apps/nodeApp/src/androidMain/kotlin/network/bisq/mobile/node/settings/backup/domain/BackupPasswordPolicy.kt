package network.bisq.mobile.node.settings.backup.domain

/**
 * The backup holds the node's identity keys and travels through the share sheet, so assume it can be
 * attacked offline without limit. Length is what protects it there; composition rules are left out on
 * purpose, as they push people towards predictable patterns. Lengths count code points, not UTF-16 units.
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

    fun findViolation(password: String): Violation? {
        val length = password.codePointCount(0, password.length)
        return when {
            length < MIN_LENGTH -> Violation.TOO_SHORT
            length > MAX_LENGTH -> Violation.TOO_LONG
            password.codePoints().distinct().count() < MIN_DISTINCT_CHARACTERS -> Violation.TOO_FEW_DISTINCT_CHARACTERS
            else -> null
        }
    }
}
