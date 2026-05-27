package network.bisq.mobile.client.payment_accounts.domain.model

interface PaymentMethod {
    val name: String
    val tradeLimitInfo: String
    val tradeDuration: String
}
