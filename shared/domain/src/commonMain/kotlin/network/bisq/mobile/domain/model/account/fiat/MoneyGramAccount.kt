package network.bisq.mobile.domain.model.account.fiat

data class MoneyGramAccount(
    override val accountName: String,
    override val accountPayload: MoneyGramAccountPayload,
) : FiatAccount
