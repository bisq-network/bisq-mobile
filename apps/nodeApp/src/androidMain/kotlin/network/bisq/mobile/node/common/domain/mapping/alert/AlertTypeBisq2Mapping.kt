package network.bisq.mobile.node.common.domain.mapping.alert

import network.bisq.mobile.domain.model.alert.AlertType
import bisq.bonded_roles.security_manager.alert.AlertType as AlertTypeBisq2

fun AlertTypeBisq2.toAlertTypeOrNull(): AlertType? =
    when (this) {
        AlertTypeBisq2.INFO -> AlertType.INFO
        AlertTypeBisq2.WARN -> AlertType.WARN
        AlertTypeBisq2.EMERGENCY -> AlertType.EMERGENCY
        AlertTypeBisq2.BAN -> AlertType.BAN
        AlertTypeBisq2.BANNED_ACCOUNT_DATA -> AlertType.BANNED_ACCOUNT_DATA
        else -> null
    }
