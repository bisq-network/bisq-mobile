package network.bisq.mobile.client.common.domain.access.session

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import network.bisq.mobile.client.common.domain.utils.ByteArrayUtils
import network.bisq.mobile.domain.utils.createUuid
import kotlin.time.Duration.Companion.minutes

class SessionToken(
    val deviceId: String,
) {
    companion object {
        const val TTL_MINUTES: Long = 15
    }

    val sessionId: String = createUuid()
    val expiresAt: Instant = Clock.System.now() + TTL_MINUTES.minutes

    private val hmacKey: ByteArray = ByteArrayUtils.randomBytes(32) // 256-bit

    fun isExpired(): Boolean = Clock.System.now() > expiresAt

    fun getHmacKey(): ByteArray = hmacKey.copyOf()
}
