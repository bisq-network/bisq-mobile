package network.bisq.mobile.client.payment_accounts.domain.model.fiat.sepa

import network.bisq.mobile.domain.model.account.create.CreatePaymentAccountPayload

data class CreateSepaAccountPayload(
    val selectedCountryCode: String,
    val acceptedCountryCodes: List<String>,
    val holderName: String,
    val iban: String,
    val bic: String,
) : CreatePaymentAccountPayload
