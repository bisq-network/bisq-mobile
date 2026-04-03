package network.bisq.mobile.domain.model.account

interface PaymentAccount {
    val accountName: String
    val accountPayload: PaymentAccountPayload
    val creationDate: String?
    val tradeLimitInfo: String?
    val tradeDuration: String?
}
