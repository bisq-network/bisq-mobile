package network.bisq.mobile.client.common.domain.service.accounts

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.account.fiat.FiatAccountVO

@Serializable
data class AddFiatAccountRequest(
    val account: FiatAccountVO,
)
