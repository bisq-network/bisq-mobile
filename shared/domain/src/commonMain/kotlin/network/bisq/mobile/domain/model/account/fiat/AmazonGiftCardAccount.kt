package network.bisq.mobile.domain.model.account.fiat

data class AmazonGiftCardAccount(
    override val accountName: String,
    override val accountPayload: AmazonGiftCardAccountPayload,
) : FiatAccount
