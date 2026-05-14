package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.EmailValidation
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
import network.bisq.mobile.data.replicated.common.validation.PhoneNumberValidation

data class ZelleAccountPayload(
    val holderName: String,
    val emailOrMobileNr: String,
    override val chargebackRisk: FiatPaymentMethodChargebackRisk? = null,
    override val paymentMethodName: String,
    override val currency: String,
    override val country: String,
) : FiatPaymentAccountPayload,
    FiatPaymentCountryBasedAccountPayload,
    FiatPaymentSingleCurrencyAccountPayload
