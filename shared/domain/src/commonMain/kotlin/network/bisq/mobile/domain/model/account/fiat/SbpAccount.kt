package network.bisq.mobile.domain.model.account.fiat

data class SbpAccount(
    override val accountName: String,
    override val accountPayload: SbpAccountPayload,
) : FiatAccount
