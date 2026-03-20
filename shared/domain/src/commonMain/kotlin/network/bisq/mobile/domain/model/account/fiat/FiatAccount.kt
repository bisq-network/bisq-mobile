package network.bisq.mobile.domain.model.account.fiat

sealed interface FiatAccount {
    val accountName: String
    val accountPayload: FiatAccountPayload
}
