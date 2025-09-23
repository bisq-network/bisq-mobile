package network.bisq.mobile.presentation.notification.model

import platform.UserNotifications.UNNotificationInterruptionLevel


fun IosNotificationInterruptionLevel.toPlatformEnum(): UNNotificationInterruptionLevel {
    return when (this) {
        IosNotificationInterruptionLevel.ACTIVE -> UNNotificationInterruptionLevel.UNNotificationInterruptionLevelActive
        IosNotificationInterruptionLevel.CRITICAL -> UNNotificationInterruptionLevel.UNNotificationInterruptionLevelCritical
        IosNotificationInterruptionLevel.PASSIVE -> UNNotificationInterruptionLevel.UNNotificationInterruptionLevelPassive
        IosNotificationInterruptionLevel.TIME_SENSITIVE -> UNNotificationInterruptionLevel.UNNotificationInterruptionLevelTimeSensitive
    }
}