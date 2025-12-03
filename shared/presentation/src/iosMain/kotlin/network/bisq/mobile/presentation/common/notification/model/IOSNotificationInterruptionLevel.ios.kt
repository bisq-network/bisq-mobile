package network.bisq.mobile.presentation.common.notification.model

import network.bisq.mobile.presentation.common.notification.model.ios.IosNotificationInterruptionLevel
import platform.UserNotifications.UNNotificationInterruptionLevel


fun IosNotificationInterruptionLevel.toPlatformEnum(): UNNotificationInterruptionLevel {
    return when (this) {
        IosNotificationInterruptionLevel.ACTIVE -> UNNotificationInterruptionLevel.UNNotificationInterruptionLevelActive
        IosNotificationInterruptionLevel.CRITICAL -> UNNotificationInterruptionLevel.UNNotificationInterruptionLevelCritical
        IosNotificationInterruptionLevel.PASSIVE -> UNNotificationInterruptionLevel.UNNotificationInterruptionLevelPassive
        IosNotificationInterruptionLevel.TIME_SENSITIVE -> UNNotificationInterruptionLevel.UNNotificationInterruptionLevelTimeSensitive
    }
}