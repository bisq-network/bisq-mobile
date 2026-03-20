package network.bisq.mobile.domain.model.account.fiat

data class PromptPayAccount(
    override val accountName: String,
    override val accountPayload: PromptPayAccountPayload,
) : FiatAccount
