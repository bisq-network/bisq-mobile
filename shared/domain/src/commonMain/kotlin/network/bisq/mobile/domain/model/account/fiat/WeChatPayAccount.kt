package network.bisq.mobile.domain.model.account.fiat

data class WeChatPayAccount(
    override val accountName: String,
    override val accountPayload: WeChatPayAccountPayload,
) : FiatAccount
