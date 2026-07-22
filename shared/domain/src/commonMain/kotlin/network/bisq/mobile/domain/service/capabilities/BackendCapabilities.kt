package network.bisq.mobile.domain.service.capabilities

/**
 * Snapshot of the trusted node's supported feature set, sourced from its `/config/capabilities`
 * manifest (see [BackendCapabilitiesService]). A node without the manifest reports no features, so
 * gating fails closed — better to hide a feature than render a broken screen against an older node.
 */
data class BackendCapabilities(
    val supportedFeatures: Set<String> = emptySet(),
) {
    fun isSupported(feature: Feature): Boolean = feature.key in supportedFeatures

    companion object {
        val UNAVAILABLE = BackendCapabilities()
    }
}
