package network.bisq.mobile.client.common.data.mapping.alert

import network.bisq.mobile.client.common.data.model.alert.AuthorizedAlertDataDto
import network.bisq.mobile.domain.model.alert.AuthorizedAlertData

fun AuthorizedAlertDataDto.toDomainOrNull(): AuthorizedAlertData? {
    val mappedType = alertType?.toAlertTypeOrNull() ?: return null
    if (!mappedType.isMessageAlert()) {
        return null
    }

    val normalizedHeadline = headline?.trim()?.takeIf { it.isNotBlank() }
    val normalizedMessage = message?.trim()?.takeIf { it.isNotBlank() }
    if (normalizedMessage == null) {
        return null
    }

    return AuthorizedAlertData(
        id = id,
        type = mappedType,
        headline = normalizedHeadline,
        message = normalizedMessage,
        haltTrading = haltTrading,
        requireVersionForTrading = requireVersionForTrading,
        minVersion = minVersion?.trim()?.takeIf { it.isNotBlank() },
        date = date,
    )
}
