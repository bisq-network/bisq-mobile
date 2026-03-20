package network.bisq.mobile.domain.model.account.fiat

data class BizumAccount(
    override val accountName: String,
    override val accountPayload: BizumAccountPayload,
) : FiatAccount
