package network.bisq.mobile.domain.data.replicated.settings

import kotlinx.serialization.Serializable

// BuildNodeConfig.APP_VERSION, BuildNodeConfig.BISQ_CORE_VERSION, BuildNodeConfig.TOR_VERSION
@Serializable
data class AboutSettingsVO(
    val appVersion: String,
    /** only available on android node */
    val bisqCoreVersion: String?,
    /** only available on android node */
    val torVersion: String?,
    /** only available on client nodes */
    val apiVersion: String?,
)