package network.bisq.mobile.client.create_payment_account

import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount

data class CreatePaymentAccountUiState(
    val paymentMethod: PaymentMethod? = null,
    val createPaymentAccount: CreatePaymentAccount? = null,
)
