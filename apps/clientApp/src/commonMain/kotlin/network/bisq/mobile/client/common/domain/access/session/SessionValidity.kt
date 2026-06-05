package network.bisq.mobile.client.common.domain.access.session

import network.bisq.mobile.domain.utils.DateUtils

object SessionValidity {
    /** Renew before server expiry to tolerate device/server clock skew and in-flight Tor handshakes. */
    const val SESSION_EXPIRY_BUFFER_MS = 30_000L

    /** Minimum remaining TTL before keeping a live WS after bootstrap POST rotates sessionId. */
    const val SESSION_SKIP_RECREATE_MIN_REMAINING_MS = 15 * 60 * 1000L

    fun hasMinRemainingValidity(
        sessionExpiresAt: Long?,
        minRemainingMs: Long = SESSION_SKIP_RECREATE_MIN_REMAINING_MS,
        now: Long = DateUtils.now(),
    ): Boolean {
        if (sessionExpiresAt == null) {
            return false
        }
        return now + minRemainingMs < sessionExpiresAt - SESSION_EXPIRY_BUFFER_MS
    }
}
