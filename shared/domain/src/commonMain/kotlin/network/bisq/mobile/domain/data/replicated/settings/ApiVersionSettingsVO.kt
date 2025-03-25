package network.bisq.mobile.domain.data.replicated.settings

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.shared.BuildConfig

@Serializable
data class ApiVersionSettingsVO(val version: String)

val apiVersionSettingsVO = ApiVersionSettingsVO(BuildConfig.BISQ_API_VERSION)
