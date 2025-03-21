package network.bisq.mobile.client.service.settings

import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO

@Serializable
data class PaymentAccountChangeRequest(
    val selectedAccount: UserDefinedFiatAccountVO,
)