package network.bisq.mobile.data.replicated.settings

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.shared.BuildConfig

@Serializable
data class ApiVersionSettingsVO(
    val version: String,
    // Only reported by containerised nodes (docker distributions): the per-image release
    // (e.g. 2.1.11.2), which advances even when the core version does not.
    val imageVersion: String? = null,
)

val apiVersionSettingsVO = ApiVersionSettingsVO(BuildConfig.BISQ_API_VERSION)
