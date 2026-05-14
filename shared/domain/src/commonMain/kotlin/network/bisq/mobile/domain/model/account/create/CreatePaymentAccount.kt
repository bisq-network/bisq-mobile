package network.bisq.mobile.domain.model.account.create

interface CreatePaymentAccount {
    val accountName: String
    val accountPayload: CreatePaymentAccountPayload
}
