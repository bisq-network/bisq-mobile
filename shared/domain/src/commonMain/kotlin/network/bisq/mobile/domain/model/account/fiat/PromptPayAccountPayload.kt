package network.bisq.mobile.domain.model.account.fiat

import network.bisq.mobile.data.replicated.common.validation.NetworkDataValidation

data class PromptPayAccountPayload(
    val promptPayId: String,
    val countryCode: String,
) : FiatAccountPayload {
    companion object {
        const val PROMPT_PAY_ID_MIN_LENGTH = 2
        const val PROMPT_PAY_ID_MAX_LENGTH = 70
    }

    init {
        verify()
    }

    fun verify() {
        NetworkDataValidation.validateCode(countryCode)
        NetworkDataValidation.validateRequiredText(promptPayId, PROMPT_PAY_ID_MIN_LENGTH, PROMPT_PAY_ID_MAX_LENGTH)
    }
}
