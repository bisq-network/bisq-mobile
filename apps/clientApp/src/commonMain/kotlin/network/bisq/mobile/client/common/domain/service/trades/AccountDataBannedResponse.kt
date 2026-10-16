package network.bisq.mobile.client.common.domain.service.trades

import kotlinx.serialization.Serializable

/** Response of the trusted node's `GET /trades/{tradeId}/account-data-banned`. */
@Serializable
data class AccountDataBannedResponse(
    val banned: Boolean,
)
