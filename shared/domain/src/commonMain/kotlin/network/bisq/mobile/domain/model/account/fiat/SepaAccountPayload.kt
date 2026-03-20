package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRailUtil
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.data.replicated.common.validation.SepaPaymentAccountValidation

data class SepaAccountPayload(
    val holderName: String,
    val iban: String,
    val bic: String,
    val countryCode: String,
    val acceptedCountryCodes: List<String>,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        PaymentAccountValidation.validateHolderName(holderName)
        SepaPaymentAccountValidation.validateSepaIban(iban, FiatPaymentRailUtil.allSepaCountryCodes)
        SepaPaymentAccountValidation.validateBic(bic)
        require(acceptedCountryCodes.isNotEmpty()) { "Accepted country codes must not be empty" }
        acceptedCountryCodes.forEach { code ->
            NetworkDataValidation.validateCode(code)
            require(FiatPaymentRailUtil.allSepaCountryCodes.contains(code)) {
                "Country code '$code' is not supported for SEPA"
            }
        }
        SepaPaymentAccountValidation.validateIbanMatchesCountryCode(iban, countryCode)
    }
}
