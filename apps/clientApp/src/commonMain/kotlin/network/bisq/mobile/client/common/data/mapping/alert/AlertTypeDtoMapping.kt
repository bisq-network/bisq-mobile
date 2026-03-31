package network.bisq.mobile.client.common.data.mapping.alert

import network.bisq.mobile.client.common.data.model.alert.AlertTypeDto
import network.bisq.mobile.domain.model.alert.AlertType

fun AlertTypeDto.toAlertTypeOrNull(): AlertType? =
    when (this) {
        AlertTypeDto.INFO -> AlertType.INFO
        AlertTypeDto.WARN -> AlertType.WARN
        AlertTypeDto.EMERGENCY -> AlertType.EMERGENCY
        AlertTypeDto.BAN -> AlertType.BAN
        AlertTypeDto.BANNED_ACCOUNT_DATA -> AlertType.BANNED_ACCOUNT_DATA
        else -> null
    }
