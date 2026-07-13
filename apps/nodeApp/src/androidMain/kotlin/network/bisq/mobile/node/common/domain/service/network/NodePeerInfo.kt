package network.bisq.mobile.node.common.domain.service.network

/**
 * Immutable snapshot of one live peer connection held by the embedded node.
 *
 * Plain Kotlin (no bisq.* types) so it can flow up to node presenters/UI without
 * leaking Bisq2 networking types. Raw data only — display formatting (e.g. the
 * "connected 3 min ago" string) is done in the composable.
 */
data class NodePeerInfo(
    val connectionId: String,
    val address: String,
    val isOutbound: Boolean,
    val establishedAtMillis: Long,
    val isSeed: Boolean,
)
