package network.bisq.mobile.data.service.bootstrap

import kotlinx.coroutines.CancellationException

/**
 * Classifies Tor bootstrap [Throwable]s for grace-period handling in [ApplicationBootstrapFacade].
 *
 * Terminal failures must surface [ApplicationBootstrapFacade.torBootstrapFailed] immediately.
 * Transient circuit-establishment errors are suppressed until the grace window elapses.
 *
 * String matching notes (kmp-tor has no structured failure kinds today):
 * - Markers validated against kmp-tor **2.6.0** and kmp-tor-resource **409.5.0** (`gradle/libs.versions.toml`).
 * - Re-check [TERMINAL_MESSAGE_MARKERS] after any kmp-tor upgrade — wording can change between releases.
 */
object TorBootstrapErrorClassification {
    fun isTerminal(error: Throwable): Boolean {
        if (error is CancellationException) {
            return false
        }
        return collectMessages(error).any(::isTerminalMessage)
    }

    internal fun isTerminalMessage(message: String): Boolean {
        val normalized = message.lowercase()
        return TERMINAL_MESSAGE_MARKERS.any { marker -> normalized.contains(marker) }
    }

    private fun collectMessages(error: Throwable): List<String> {
        val messages = mutableListOf<String>()
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = error
        while (current != null && seen.add(current)) {
            current.message?.let { messages.add(it) }
            current = current.cause
        }
        return messages
    }

    private val TERMINAL_MESSAGE_MARKERS =
        listOf(
            "ctrlconnection stream ended",
            "daemon stopped",
            "control connection",
            "tor process exited",
        )
}
