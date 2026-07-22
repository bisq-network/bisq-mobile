package network.bisq.mobile.domain.service.capabilities

/**
 * Recent bisq2 API features the app can gate its UI on. Each [key] MUST match a key in bisq2's
 * `ApiFeature` enum (served by the node's `/config/capabilities` manifest). A node that advertises the
 * manifest is authoritative; a node without the manifest falls back to [LEGACY_BASELINE_KEYS].
 *
 * Add an entry only for a feature that some reachable nodes may lack; long-established behaviour does
 * not belong here.
 */
enum class Feature(
    val key: String,
) {
    CLOSED_TRADES("closed-trades"),
    // TODO Add NETWORK_INFO("network-info") and any other new bisq2 dependent feature
    ;

    companion object {
        /**
         * Features that shipped BEFORE the `/config/capabilities` manifest existed. A node paired
         * today may already run such a feature yet not serve the manifest (it predates the config
         * endpoint on the rollout path), so these are ASSUMED present when the manifest is absent —
         * otherwise the feature would vanish for users until every node updates. A node that genuinely
         * lacks even these degrades gracefully (the screen just shows nothing).
         *
         * Newer features are NOT listed here: they ship together with the manifest, so a missing
         * manifest correctly hides them.
         *
         * TODO this should be removed along with usages in the near future
         */
        val LEGACY_BASELINE_KEYS: Set<String> = setOf(CLOSED_TRADES.key)
    }
}
