package network.bisq.mobile.client.payment_accounts.data.model.bank_account_country_details

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.BankAccountCountryDetailsDto

@Serializable
data class BankAccountCountryDetailsCache(
    val apiVersion: String = "",
    val detailsByCountryCode: Map<String, BankAccountCountryDetailsDto> = emptyMap(),
)
