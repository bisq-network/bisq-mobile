package network.bisq.mobile.client.common.domain.service.accounts.user_defined

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.fiat.user_defined.CreateUserDefinedFiatAccountDto

@Serializable
data class SaveUserDefinedAccountRequest(
    val account: CreateUserDefinedFiatAccountDto,
)
