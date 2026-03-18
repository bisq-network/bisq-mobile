package network.bisq.mobile.client.common.domain.service.accounts

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.replicated.api.dto.account.fiat.FiatAccountDto

@Serializable
data class AddFiatAccountRequest(
    val account: FiatAccountDto,
)
