package network.bisq.mobile.client.payment_accounts.data.mapping.crypto

import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentMethodDto
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod

fun CryptoPaymentMethodDto.toDomain(): CryptoPaymentMethod =
    CryptoPaymentMethod(
        code = code,
        name = name,
        supportAutoConf = supportAutoConf,
        tradeLimitInfo = tradeLimitInfo,
        tradeDuration = tradeDuration,
    )
