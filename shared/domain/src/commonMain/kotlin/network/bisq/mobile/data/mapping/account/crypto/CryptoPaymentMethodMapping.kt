package network.bisq.mobile.data.mapping.account.crypto

import network.bisq.mobile.data.model.account.crypto.CryptoPaymentMethodDto
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod

fun CryptoPaymentMethodDto.toDomain(): CryptoPaymentMethod =
    CryptoPaymentMethod(
        code = code,
        name = name,
        category = category,
        supportAutoConf = supportAutoConf,
    )
