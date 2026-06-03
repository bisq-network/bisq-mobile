package network.bisq.mobile.client.payment_accounts.data.mapping.fiat

import network.bisq.mobile.client.payment_accounts.data.model.fiat.common.CountryDto
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country

fun CountryDto.toDomain(): Country =
    Country(
        code = code,
        name = name,
    )
