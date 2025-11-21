package network.bisq.mobile.client.common.domain.service.accounts

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO

@Serializable
data class PaymentAccountChangeRequest(
    val selectedAccount: UserDefinedFiatAccountVO,
)