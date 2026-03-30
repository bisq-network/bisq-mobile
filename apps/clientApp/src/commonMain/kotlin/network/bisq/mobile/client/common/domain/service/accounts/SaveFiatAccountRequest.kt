package network.bisq.mobile.client.common.domain.service.accounts

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.PaymentAccountDto

@Serializable
data class SaveFiatAccountRequest(
    val account: PaymentAccountDto,
)
