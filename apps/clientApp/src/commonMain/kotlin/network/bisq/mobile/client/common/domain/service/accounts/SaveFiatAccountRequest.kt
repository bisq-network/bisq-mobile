package network.bisq.mobile.client.common.domain.service.accounts

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.fiat.FiatAccountDto

@Serializable
data class SaveFiatAccountRequest(
    val account: FiatAccountDto,
)
