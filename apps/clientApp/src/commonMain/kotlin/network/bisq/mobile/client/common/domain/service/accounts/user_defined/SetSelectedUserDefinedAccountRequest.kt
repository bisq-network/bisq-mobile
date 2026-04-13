package network.bisq.mobile.client.common.domain.service.accounts.user_defined

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto

@Serializable
data class SetSelectedUserDefinedAccountRequest(
    val selectedAccount: UserDefinedFiatAccountDto,
)
