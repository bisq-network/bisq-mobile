package network.bisq.mobile.domain.service.capabilities

/**
 * Backend feature capabilities determined by probing the trusted Bisq2 node's
 * actual REST surface. See [BackendCapabilitiesService] for the workflow.
 *
 * To add a new gated capability, append a boolean field with a KDoc that
 * names the bisq2 PR / release that introduced the endpoint.
 */
data class BackendCapabilities(
    /**
     * `GET /trades/closed` paginated REST endpoint + `CLOSED_TRADES`
     * WebSocket topic. Introduced in bisq2 PR #4700.
     */
    val hasClosedTradesApi: Boolean = false,
) {
    companion object {
        val UNAVAILABLE = BackendCapabilities()
    }
}
