package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

private const val IMPS_ACCOUNT_NR_MIN_LENGTH = 1
private const val IMPS_ACCOUNT_NR_MAX_LENGTH = 50
private const val IMPS_IFSC_MIN_LENGTH = 1
private const val IMPS_IFSC_MAX_LENGTH = 50

data class ImpsAccountPayload(
    val holderName: String,
    val accountNr: String,
    val ifsc: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode("IN")
        PaymentAccountValidation.validateHolderName(holderName)
        NetworkDataValidation.validateRequiredText(accountNr, IMPS_ACCOUNT_NR_MIN_LENGTH, IMPS_ACCOUNT_NR_MAX_LENGTH)
        NetworkDataValidation.validateRequiredText(ifsc, IMPS_IFSC_MIN_LENGTH, IMPS_IFSC_MAX_LENGTH)
    }
}
