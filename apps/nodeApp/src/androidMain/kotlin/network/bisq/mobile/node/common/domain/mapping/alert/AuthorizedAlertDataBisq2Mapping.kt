package network.bisq.mobile.node.common.domain.mapping.alert

import network.bisq.mobile.domain.model.alert.AuthorizedAlertData
import kotlin.jvm.optionals.getOrNull
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData as AuthorizedAlertDataBisq2

fun AuthorizedAlertDataBisq2.toDomainOrNull(): AuthorizedAlertData? {
    val mappedType = alertType.toAlertTypeOrNull() ?: return null
    if (!mappedType.isMessageAlert()) {
        return null
    }

    val normalizedHeadline = headline.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    val normalizedMessage = message.getOrNull()?.trim()?.takeIf { it.isNotBlank() }
    if (normalizedMessage == null) {
        return null
    }

    return AuthorizedAlertData(
        id = id,
        type = mappedType,
        headline = normalizedHeadline,
        message = normalizedMessage,
        haltTrading = isHaltTrading,
        requireVersionForTrading = isRequireVersionForTrading,
        minVersion = minVersion.getOrNull()?.trim()?.takeIf { it.isNotBlank() },
        date = date,
    )
}
