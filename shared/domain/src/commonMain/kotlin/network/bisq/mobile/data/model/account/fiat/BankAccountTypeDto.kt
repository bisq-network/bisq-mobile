package network.bisq.mobile.data.model.account.fiat

import kotlinx.serialization.Serializable

@Serializable
enum class BankAccountTypeDto {
    CHECKING,
    SAVINGS,
}
