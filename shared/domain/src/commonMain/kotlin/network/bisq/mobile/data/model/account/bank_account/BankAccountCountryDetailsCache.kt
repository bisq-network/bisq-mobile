package network.bisq.mobile.data.model.account.bank_account

import kotlinx.serialization.Serializable
import network.bisq.mobile.data.model.account.fiat.BankAccountCountryDetailsDto

@Serializable
data class BankAccountCountryDetailsCache(
    val apiVersion: String = "",
    val detailsByCountryCode: Map<String, BankAccountCountryDetailsDto> = emptyMap(),
)
