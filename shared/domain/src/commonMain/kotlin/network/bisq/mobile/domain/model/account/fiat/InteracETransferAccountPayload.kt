package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.EmailValidation
import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation

data class InteracETransferAccountPayload(
    val holderName: String,
    val email: String,
    val question: String,
    val answer: String,
) : FiatAccountPayload {
    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode("CA")
        PaymentAccountValidation.validateHolderName(holderName)
        require(EmailValidation.isValid(email)) { "Email is invalid" }
        require(question.isNotBlank()) { "question must not be blank" }
        require(answer.isNotBlank()) { "answer must not be blank" }
    }
}
