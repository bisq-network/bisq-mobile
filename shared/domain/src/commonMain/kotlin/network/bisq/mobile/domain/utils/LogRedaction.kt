package network.bisq.mobile.domain.utils

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity

/**
 * Opt-in for exceptions that carry facts safe to put in a log line: a status code, an attempt
 * count, a bounded kind. Never the message, which is where remote bodies and user data travel.
 */
interface LogRedactable {
    /** Short `key=value` facts, or null when there is nothing safe to add. */
    fun redactedDetails(): String?
}

private const val MAX_CAUSE_DEPTH = 5
private const val CAUSE_SEPARATOR = " <- "

/**
 * Log-safe description of this throwable and its cause chain: class names plus any
 * [LogRedactable] details, never messages. `WebSocketRestApiException(http=400) <- IOException`.
 *
 * Messages are left out on purpose rather than filtered: on Bisq Connect they carry the trusted
 * node's response body, on the node app the peer's protocol text, and serialization errors quote
 * their input. A log line that needs more than the class chain names the fact it needs explicitly.
 */
fun Throwable.redactedSummary(): String {
    val parts = ArrayList<String>(2)
    val visited = ArrayList<Throwable>(2)
    var current: Throwable? = this
    while (current != null && parts.size < MAX_CAUSE_DEPTH && visited.none { it === current }) {
        visited += current
        parts += current.redactedName()
        current = current.cause
    }
    return parts.joinToString(CAUSE_SEPARATOR)
}

private fun Throwable.redactedName(): String {
    val name = this::class.simpleName ?: "UnknownThrowable"
    val details = (this as? LogRedactable)?.redactedDetails()?.takeIf { it.isNotBlank() }
    return if (details == null) name else "$name($details)"
}

/**
 * Release-build log writer: forwards the message, appends the throwable's [redactedSummary] and
 * passes no throwable on. The platform writers print a stack trace otherwise, and a stack trace
 * starts with `toString()` of every cause, which is class name plus message. This is the safety
 * net under the call sites; it does not excuse a call site from leaving messages out of its text.
 */
class RedactingLogWriter(
    private val delegate: LogWriter,
) : LogWriter() {
    override fun isLoggable(
        tag: String,
        severity: Severity,
    ): Boolean = delegate.isLoggable(tag, severity)

    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        val line = if (throwable == null) message else "$message | ${throwable.redactedSummary()}"
        delegate.log(severity, line, tag, null)
    }
}
