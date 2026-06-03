package network.bisq.mobile.client.payment_accounts.data.model.fiat.sepa

import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.data.model.create.CreatePaymentAccountPayloadDto

@Serializable
data class CreateSepaAccountPayloadDto(
    val selectedCountryCode: String,
    val acceptedCountryCodes: List<String>,
    val holderName: String,
    val iban: String,
    val bic: String,
) : CreatePaymentAccountPayloadDto
