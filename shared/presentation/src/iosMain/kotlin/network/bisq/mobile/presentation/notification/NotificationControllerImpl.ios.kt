package network.bisq.mobile.presentation.notification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.notification.model.NotificationConfig
import network.bisq.mobile.presentation.notification.model.NotificationPressAction
import network.bisq.mobile.presentation.notification.model.toPlatformEnum
import network.bisq.mobile.presentation.ui.navigation.Routes
import platform.Foundation.NSNumber
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NotificationControllerImpl : NotificationController, Logging {
    private val logScope = CoroutineScope(Dispatchers.Main)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun hasPermission(): Boolean = suspendCoroutine { continuation ->
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                val status = settings?.authorizationStatus
                val isGranted = status == UNAuthorizationStatusAuthorized
                continuation.resume(isGranted)
            }
    }

    override fun notify(config: NotificationConfig) {
        log.i { "iOS pushNotification called - title: '$config.title', body: '$config.body'" }

        if (config.skipInForeground && isAppInForeground()) {
            log.w { "Skipping notification since app is in the foreground and skipInForeground is true" }
            return
        }

        val content = UNMutableNotificationContent().apply {
            config.title?.let { setTitle(it) }
            config.subtitle?.let { setSubtitle(it) }
            config.body?.let { setBody(it) }
            config.badgeCount?.let { setBadge(NSNumber(it)) }
            configureSound(this, config)
            config.ios?.interruptionLevel?.let {
                setInterruptionLevel(it.toPlatformEnum())
            }
            configureActions(this, config)
        }

        val requestId = config.id
        val request = UNNotificationRequest.requestWithIdentifier(
            requestId,
            content,
            null,
        )
        UNUserNotificationCenter.currentNotificationCenter()
            .addNotificationRequest(request) { error ->
                if (error != null) {
                    logDebug("Error adding notification request: ${error.localizedDescription}")
                } else {
                    logDebug("Notification $requestId added successfully")
                }
            }
    }

    override fun cancel(id: String) {
        UNUserNotificationCenter.currentNotificationCenter().apply {
            removePendingNotificationRequestsWithIdentifiers(listOf(id))
            removeDeliveredNotificationsWithIdentifiers(listOf(id))
        }
        logDebug("Notification $id cancelled")
    }


    override fun isAppInForeground(): Boolean {
        // for iOS we handle this externally
        return false
    }

    private fun logDebug(message: String) {
        logScope.launch { // (Dispatchers.Main)
            log.d { message }
        }
    }

    private fun configureSound(content: UNMutableNotificationContent, config: NotificationConfig) {
        val soundName = config.ios?.sound
        val isCritical = config.ios?.critical == true
        val criticalVolume = config.ios?.criticalVolume
        when (soundName) {
            "default" -> {
                if (isCritical) {
                    if (criticalVolume != null) {
                        content.setSound(
                            UNNotificationSound.defaultCriticalSoundWithAudioVolume(
                                criticalVolume
                            )
                        )
                    } else {
                        content.setSound(UNNotificationSound.defaultCriticalSound())
                    }
                } else {
                    content.setSound(UNNotificationSound.defaultSound())
                }
            }

            null -> {
                content.setSound(null)
            }

            else -> {
                if (isCritical) {
                    if (criticalVolume != null) {
                        content.setSound(
                            UNNotificationSound.criticalSoundNamed(
                                soundName,
                                criticalVolume
                            )
                        )
                    } else {
                        content.setSound(UNNotificationSound.criticalSoundNamed(soundName))
                    }
                } else {
                    content.setSound(UNNotificationSound.soundNamed(soundName))
                }
            }
        }
    }

    private fun configureActions(content: UNMutableNotificationContent, config: NotificationConfig) {
        val actions = mutableListOf<UNNotificationAction>()
        val userInfo = mutableMapOf<Any?, Any>()
        config.ios?.actions?.forEachIndexed { index, action ->
            val pressAction = action.pressAction
            when (pressAction) {
                is NotificationPressAction.Route,
                is NotificationPressAction.Default -> {
                    val uri =
                        Routes.getDeeplinkUriString(
                            if (pressAction is NotificationPressAction.Route)
                                pressAction.route
                            else Routes.TabOpenTradeList
                        )
                    val unAction = UNNotificationAction.actionWithIdentifier(
                        "route_$index",
                        action.title,
                        UNNotificationActionOptionForeground
                    )
                    actions.add(unAction)
                    userInfo["route_$index"] = uri
                }
            }
        }
        if (actions.isNotEmpty()) {
            val categoryId = config.id
            // create category with actions
            val category = UNNotificationCategory.categoryWithIdentifier(
                categoryId,
                actions,
                emptyList<String>(),
                UNNotificationCategoryOptionNone,
            )
            // Register the category with the notification center
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.getNotificationCategoriesWithCompletionHandler { existing ->
                // Merge existing categories with the new one
                val newCategory = setOf(category)
                if (existing != null) {
                    center.setNotificationCategories(existing + newCategory)
                } else {
                    center.setNotificationCategories(newCategory)
                }
            }
            content.setCategoryIdentifier(categoryId)
            content.setUserInfo(userInfo)
        }
    }
}
