package network.bisq.mobile.presentation.create_payment_account.select_payment_method.model

import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO

interface PaymentMethodVO {
    val paymentType: PaymentTypeVO
    val name: String
}
