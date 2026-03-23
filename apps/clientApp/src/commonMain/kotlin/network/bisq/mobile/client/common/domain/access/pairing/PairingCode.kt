package network.bisq.mobile.client.common.domain.access.pairing

import kotlin.time.Instant

data class PairingCode(
    val id: String,
    val expiresAt: Instant,
    val grantedPermissions: Set<Permission>,
) {
    companion object {
        const val VERSION: Byte = 1
    }
}
