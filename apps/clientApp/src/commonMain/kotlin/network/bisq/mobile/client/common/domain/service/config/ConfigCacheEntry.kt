package network.bisq.mobile.client.common.domain.service.config

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.crypto.getSha256
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO

/**
 * Persisted snapshot of the trusted node's static config, tagged with a hash of the node it came from
 * and the node's API version. The cache is only reused when both [trustedNodeHostHash] and
 * [apiVersion] match the currently paired node — static config changes only with the API version, and
 * a user may pair more than one node, so the version alone is not a safe key.
 *
 * The host is stored as a hash (not the raw onion): the trusted-node address otherwise lives only in
 * the encrypted SensitiveSettings store, so we avoid writing a plaintext copy here while still keying
 * per node.
 */
@Serializable
data class ConfigCacheEntry(
    val trustedNodeHostHash: String,
    val apiVersion: String,
    val tradeAmountLimits: TradeAmountLimitsVO,
    val supportedFeatures: Set<String> = emptySet(),
)

@OptIn(ExperimentalStdlibApi::class)
fun hashTrustedNodeHost(host: String): String = getSha256(host.encodeToByteArray()).toHexString()
