package network.bisq.mobile.domain.model.account.fiat

data class ZelleAccount(
    override val accountName: String,
    override val accountPayload: ZelleAccountPayload,
) : FiatAccount
