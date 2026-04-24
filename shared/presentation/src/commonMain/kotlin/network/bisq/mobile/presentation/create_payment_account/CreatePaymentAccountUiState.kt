package network.bisq.mobile.presentation.create_payment_account

import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.PaymentMethodVO

data class CreatePaymentAccountUiState(
    val paymentMethod: PaymentMethodVO? = null,
    val paymentAccount: PaymentAccount? = null,
)
