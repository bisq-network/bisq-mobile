package network.bisq.mobile.client.payment_accounts.domain.service

class PaymentAccountNameAlreadyExistsException(
    message: String? = null,
) : Exception(message)
