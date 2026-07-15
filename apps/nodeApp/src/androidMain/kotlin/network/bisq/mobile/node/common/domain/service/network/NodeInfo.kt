package network.bisq.mobile.node.common.domain.service.network

/**
 * Immutable snapshot of the embedded node's own identity.
 *
 * Plain Kotlin (no bisq.* types) so it can flow up to node presenters/UI without
 * leaking Bisq2 networking types. Fields are nullable because they resolve at
 * different times: [keyId] is available as soon as the default node is set, while
 * [onionAddress] only resolves once the server socket binds.
 */
data class NodeInfo(
    val onionAddress: String? = null,
    val keyId: String? = null,
)
