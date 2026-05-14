package network.bisq.mobile.domain.model.account

interface PaymentMethod {
    val name: String
    val tradeLimitInfo: String
    val tradeDuration: String
}
