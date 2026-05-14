package network.bisq.mobile.client.common.domain.service.accounts.user_defined

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.fiat.create.CreateUserDefinedFiatAccountDto

@Serializable
data class AddUserDefinedAccountRequest(
    val account: CreateUserDefinedFiatAccountDto,
)
