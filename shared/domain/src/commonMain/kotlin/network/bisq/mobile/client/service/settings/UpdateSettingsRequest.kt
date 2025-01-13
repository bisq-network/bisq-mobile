package network.bisq.mobile.client.service.settings

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSettingsRequest(
    val key: SettingsKey,
    val value: SettingsChangeRequest,
)