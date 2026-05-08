package network.bisq.mobile.domain.service.capabilities

import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for backend (trusted Bisq2 node) feature capabilities.
 *
 * Capabilities are determined by **probing the actual feature endpoint**, not
 * by reading the node's reported version. This is intentional:
 *
 * - The `/settings/version` string can be misleading (e.g. headless / custom
 *   builds, feature backports, dev branches reporting a stable version).
 * - Probing reflects the real surface area available to mobile, regardless of
 *   how the node identifies itself.
 *
 * A probe is "is this endpoint reachable AND does it return a parseable
 * response?". Network errors and 404s alike resolve to *unsupported* (fail
 * closed — better to hide a feature than render a broken screen).
 *
 * ## How to gate a new feature
 *
 * 1. Add a `val newFeatureCapability: Boolean = false` field to
 *    [BackendCapabilities] with a KDoc naming the bisq2 PR / release
 *    that introduced the API.
 * 2. In the implementation's `refresh()`, add a probe call to the relevant
 *    API gateway with a minimal-cost request (e.g. `page=1&pageSize=1`,
 *    or a HEAD request) and map the `Result.isSuccess` into the new field.
 * 3. Read it in your presenter via `koinInject<BackendCapabilitiesService>()`
 *    and gate UI on `capabilities.value.newFeatureCapability`.
 * 4. When the API is universally available (e.g. the bisq2 minimum supported
 *    version everywhere includes it), the gate becomes dead code and can be
 *    removed.
 *
 * Probes run on every connectivity transition into a connected state.
 */
interface BackendCapabilitiesService {
    val capabilities: StateFlow<BackendCapabilities>

    /**
     * Probe all features and update [capabilities]. Idempotent; safe to call
     * repeatedly. Triggered automatically by the implementation on connection
     * transitions; UI code should not normally need to call this.
     */
    suspend fun refresh()
}
