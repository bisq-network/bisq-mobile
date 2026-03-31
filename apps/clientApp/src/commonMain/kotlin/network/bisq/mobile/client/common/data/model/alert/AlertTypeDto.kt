package network.bisq.mobile.client.common.data.model.alert

import kotlinx.serialization.Serializable

@Serializable
enum class AlertTypeDto {
    INFO,
    WARN,
    EMERGENCY,
    BAN,
    BANNED_ACCOUNT_DATA,
}
