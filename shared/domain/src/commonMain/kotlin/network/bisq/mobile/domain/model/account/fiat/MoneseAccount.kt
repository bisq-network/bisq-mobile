package network.bisq.mobile.domain.model.account.fiat

data class MoneseAccount(
    override val accountName: String,
    override val accountPayload: MoneseAccountPayload,
) : FiatAccount
