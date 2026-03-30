package network.bisq.mobile.domain.model.account

interface PaymentAccount {
    val accountName: String
    val accountPayload: PaymentAccountPayload
    val creationDate: Long?
}
