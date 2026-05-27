package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.FiatCurrencyDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency

internal fun FiatCurrencyDto.toDomain(): FiatCurrency = FiatCurrency(code = code, name = name)
