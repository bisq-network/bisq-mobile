package network.bisq.mobile.presentation.common.notification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.AppForegroundController
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.common.notification.model.NotificationButton
import network.bisq.mobile.presentation.common.notification.model.NotificationConfig
import network.bisq.mobile.presentation.common.notification.model.NotificationPressAction
import network.bisq.mobile.presentation.common.notification.model.ios.IosNotificationCategory
import network.bisq.mobile.presentation.common.notification.model.toPlatformEnum
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import platform.Foundation.NSNumber
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NotificationControllerImpl(
    private val appForegroundController: AppForegroundController,
) : NotificationController,
    Logging {
    private val logScope = CoroutineScope(Dispatchers.Main)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun hasPermission(): Boolean =
        suspendCoroutine { continuation ->
            UNUserNotificationCenter
                .currentNotificationCenter()
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

        val content =
            UNMutableNotificationContent().apply {
                config.title?.let { setTitle(it) }
                config.subtitle?.let { setSubtitle(it) }
                config.body?.let { setBody(it) }
                config.badgeCount?.let { setBadge(NSNumber(it)) }
                configureSound(this, config)
                config.ios?.interruptionLevel?.let {
                    setInterruptionLevel(it.toPlatformEnum())
                }
                config.ios?.categoryId?.let {
                    val actions = config.ios.actions
                    if (actions.isNullOrEmpty()) {
                        throw IllegalArgumentException("When setting categoryId, notification actions must be provided to behave correctly")
                    }
                    setCategoryIdentifier(it)
                    configureActions(this, actions)
                }
                config.ios?.actions?.let {
                    if (config.ios.categoryId == null) {
                        throw IllegalArgumentException("When setting actions, notification categoryId must be provided to behave correctly")
                    }
                }
                config.ios?.pressAction?.let {
                    addActionUserInfo(this, it, "default")
                }

                if (config.skipInForeground) {
                    setUserInfo(this.userInfo + ("skipForeground" to 1))
                }
            }

        val requestId = config.id
        val request =
            UNNotificationRequest.requestWithIdentifier(
                requestId,
                content,
                null,
            )

        // Remove any NSE-delivered push notifications before posting the richer local
        // notification. The NSE shows a privacy-safe generic message (e.g. "Trade update");
        // once the app wakes up and has full context, we replace it with detailed content.
        log.d { "notify(): removing NSE notifications before posting local '$requestId'" }
        removeNseDeliveredNotifications {
            UNUserNotificationCenter
                .currentNotificationCenter()
                .addNotificationRequest(request) { error ->
                    if (error != null) {
                        logDebug("Error adding notification request: ${error.localizedDescription}")
                    } else {
                        logDebug("Notification $requestId added successfully")
                    }
                }
        }
    }

    /**
     * Finds and removes delivered notifications that were posted by the Notification
     * Service Extension (identified by `nse_decrypted: true` in userInfo), then
     * invokes [onComplete]. This prevents duplicate notifications when the app posts
     * a richer local notification to replace the NSE's privacy-safe placeholder.
     */
    private fun removeNseDeliveredNotifications(onComplete: () -> Unit) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.getDeliveredNotificationsWithCompletionHandler { delivered ->
            val allNotifications = delivered?.mapNotNull { it as? UNNotification } ?: emptyList()
            log.d { "Delivered notifications count: ${allNotifications.size}" }
            allNotifications.forEach { notification ->
                val userInfo = notification.request.content.userInfo
                log.d {
                    "  Notification id=${notification.request.identifier}, " +
                        "title='${notification.request.content.title}', " +
                        "nse_decrypted=${userInfo["nse_decrypted"]}, " +
                        "keys=${userInfo.keys.joinToString()}"
                }
            }

            val nseIds =
                allNotifications
                    .filter { it.request.content.userInfo["nse_decrypted"] != null }
                    .map { it.request.identifier }

            if (nseIds.isNotEmpty()) {
                log.d { "Removing ${nseIds.size} NSE-delivered notification(s): $nseIds" }
                center.removeDeliveredNotificationsWithIdentifiers(nseIds)
            } else {
                log.d { "No NSE notifications found to remove" }
            }
            onComplete()
        }
    }

    override fun clearPreRenderedNotifications() {
        log.i { "clearPreRenderedNotifications called (app entered foreground)" }
        removeNseDeliveredNotifications {
            log.i { "Pre-rendered (NSE) notifications cleared on foreground entry" }
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
        // for iOS we handle this inside delegate
        // but wouldn't hurt to return early when possible
        return appForegroundController.isForeground.value
    }

    private fun logDebug(message: String) {
        logScope.launch {
            // (Dispatchers.Main)
            log.d { message }
        }
    }

    private fun configureSound(
        content: UNMutableNotificationContent,
        config: NotificationConfig,
    ) {
        val soundName = config.ios?.sound
        val isCritical = config.ios?.critical == true
        val criticalVolume = config.ios?.criticalVolume
        when (soundName) {
            "default" -> {
                if (isCritical) {
                    if (criticalVolume != null) {
                        content.setSound(
                            UNNotificationSound.defaultCriticalSoundWithAudioVolume(
                                criticalVolume,
                            ),
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
                                criticalVolume,
                            ),
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

    private fun configureActions(
        content: UNMutableNotificationContent,
        actions: List<NotificationButton>,
    ) {
        for (action in actions) {
            val pressAction = action.pressAction
            addActionUserInfo(content, pressAction)
        }
    }

    private fun addActionUserInfo(
        content: UNMutableNotificationContent,
        pressAction: NotificationPressAction,
        id: String? = null,
    ) {
        val id = id ?: pressAction.id
        when (pressAction) {
            is NotificationPressAction.Route -> {
                content.setUserInfo(
                    content.userInfo + (id to pressAction.route.toUriString()),
                )
            }

            is NotificationPressAction.Default -> {
                content.setUserInfo(
                    content.userInfo + (id to NavRoute.TabOpenTradeList.toUriString()),
                )
            }
        }
    }

    private fun setNotificationCategories(categories: Set<IosNotificationCategory>) {
        val resultCategories = mutableSetOf<UNNotificationCategory>()
        for (cat in categories) {
            val actions = mutableListOf<UNNotificationAction>()
            cat.actions.forEachIndexed { index, action ->
                val pressAction = action.pressAction
                when (pressAction) {
                    is NotificationPressAction.Route,
                    is NotificationPressAction.Default,
                    -> {
                        val unAction =
                            UNNotificationAction.actionWithIdentifier(
                                action.pressAction.id,
                                action.title,
                                UNNotificationActionOptionForeground,
                            )
                        actions.add(unAction)
                    }
                }
            }
            if (actions.isNotEmpty()) {
                // create category with actions
                val category =
                    UNNotificationCategory.categoryWithIdentifier(
                        cat.id,
                        actions,
                        emptyList<String>(),
                        UNNotificationCategoryOptionNone,
                    )
                resultCategories.add(category)
            }
        }

        if (resultCategories.isNotEmpty()) {
            UNUserNotificationCenter
                .currentNotificationCenter()
                .setNotificationCategories(resultCategories)
        }
    }

    // NOTE: The UNUserNotificationCenter delegate is now set in Swift's AppDelegate
    // (didFinishLaunchingWithOptions) to ensure it's available before iOS delivers any
    // pending notification responses. Previously it was set here in Kotlin, but that was
    // too late — tapping a notification while the app was terminated caused the response
    // to be silently dropped. The Swift delegate handles both didReceiveNotificationResponse
    // (deep linking via ExternalUriHandler) and willPresent (foreground presentation).

    private fun setupNotificationCategories() {
        setNotificationCategories(
            // theres no need for this right now but I'm leaving it here as an example
            setOf(
//                IosNotificationCategory(
//                    id = NotificationChannels.TRADE_UPDATES,
//                    actions = listOf(
//                        NotificationButton(
//                            title = "mobile.action.notifications.openTrade".i18n(),
//                            // the actual route here doesn't matter, but it will matter
//                            // when actions are passed to notify()
//                            pressAction = NotificationPressAction.Route(Routes.TabHome)
//                        )
//                    )
//                ),
            ),
        )
    }

    @Suppress("unused") // Called from iosClient.swift
    fun setup() {
        setupNotificationCategories()
    }
}
