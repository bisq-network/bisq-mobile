package network.bisq.mobile.client.payment_accounts.data.model.fiat.common

import kotlinx.serialization.Serializable

@Serializable
enum class BankAccountTypeDto {
    CHECKING,
    SAVINGS,
}
