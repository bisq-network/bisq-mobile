package network.bisq.mobile.client.common.data.model.alert

import kotlinx.serialization.Serializable

@Serializable
data class AuthorizedAlertDataDto(
    val id: String,
    val alertType: AlertTypeDto? = null,
    val headline: String? = null,
    val message: String? = null,
    val haltTrading: Boolean = false,
    val requireVersionForTrading: Boolean = false,
    val minVersion: String? = null,
    val date: Long,
)
