package network.bisq.mobile.domain.service.capabilities

/**
 * Snapshot of the feature set the trusted node advertises (see [BackendCapabilitiesService]). A node
 * that advertises an empty set has no features, so gating fails closed — better to hide a feature than
 * render a broken screen against an older node. A node that advertises nothing at all (no `/info`
 * features and no manifest) gets [Feature.LEGACY_BASELINE_KEYS] instead.
 */
data class BackendCapabilities(
    val supportedFeatures: Set<String> = emptySet(),
) {
    fun isSupported(feature: Feature): Boolean = feature.key in supportedFeatures

    companion object {
        val UNAVAILABLE = BackendCapabilities()
    }
}
