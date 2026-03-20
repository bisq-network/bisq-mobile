package network.bisq.mobile.domain.model.account.fiat

data class UserDefinedFiatAccount(
    override val accountName: String,
    override val accountPayload: UserDefinedFiatAccountPayload,
) : FiatAccount
