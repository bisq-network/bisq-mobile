package network.bisq.mobile.data.mapping.account.fiat

import network.bisq.mobile.data.model.account.fiat.FiatCurrencyDto
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency

internal fun FiatCurrencyDto.toDomain(): FiatCurrency = FiatCurrency(code = code, name = name)
