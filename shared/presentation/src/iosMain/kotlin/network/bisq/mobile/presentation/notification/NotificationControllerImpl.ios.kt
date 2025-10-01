package network.bisq.mobile.presentation.notification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.notification.model.IosNotificationCategory
import network.bisq.mobile.presentation.notification.model.NotificationButton
import network.bisq.mobile.presentation.notification.model.NotificationConfig
import network.bisq.mobile.presentation.notification.model.NotificationPressAction
import network.bisq.mobile.presentation.notification.model.toPlatformEnum
import network.bisq.mobile.presentation.ui.navigation.ExternalUriHandler
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import platform.Foundation.NSNumber
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationDefaultActionIdentifier
import platform.UserNotifications.UNNotificationPresentationOptionAlert
import platform.UserNotifications.UNNotificationPresentationOptionBadge
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class NotificationControllerImpl(
    private val appForegroundController: AppForegroundController,
) : NotificationController, Logging {
    private val logScope = CoroutineScope(Dispatchers.Main)

    // strong reference to delegate to keep it in memory and working
    private var notificationDelegate: NSObject? = null

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
        // for iOS we handle this inside delegate
        // but wouldn't hurt to return early when possible
        return appForegroundController.isForeground.value
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

    private fun configureActions(
        content: UNMutableNotificationContent,
        actions: List<NotificationButton>
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
                    content.userInfo + (id to pressAction.route.toUriString())
                )
            }

            is NotificationPressAction.Default -> {
                content.setUserInfo(
                    content.userInfo + (id to NavRoute.TabOpenTradeList.toUriString())
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
                    is NotificationPressAction.Default -> {
                        val unAction = UNNotificationAction.actionWithIdentifier(
                            action.pressAction.id,
                            action.title,
                            UNNotificationActionOptionForeground
                        )
                        actions.add(unAction)
                    }
                }
            }
            if (actions.isNotEmpty()) {
                // create category with actions
                val category = UNNotificationCategory.categoryWithIdentifier(
                    cat.id,
                    actions,
                    emptyList<String>(),
                    UNNotificationCategoryOptionNone,
                )
                resultCategories.add(category)
            }
        }

        if (resultCategories.isNotEmpty()) {
            UNUserNotificationCenter.currentNotificationCenter()
                .setNotificationCategories(resultCategories)
        }
    }

    private fun setupDelegate() {
        val delegate = object : NSObject(), UNUserNotificationCenterDelegateProtocol {
            // Handle user actions on the notification
            override fun userNotificationCenter(
                center: UNUserNotificationCenter,
                didReceiveNotificationResponse: UNNotificationResponse,
                withCompletionHandler: () -> Unit
            ) {
                // Handle the response when the user taps the notification
                val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
                val actionId =
                    didReceiveNotificationResponse.actionIdentifier.let {
                        if (it == UNNotificationDefaultActionIdentifier) "default" else it
                    }
                when (actionId) {
                    "default",
                    "route" -> {
                        val userInfoMap = userInfo as? Map<*, *>
                        val uri = userInfoMap?.get(actionId) as? String
                        if (uri != null) {
                            ExternalUriHandler.onNewUri(uri)
                        }
                    }
                }
                withCompletionHandler()
            }

            // Asks the delegate how to handle a notification that arrived while
            // the app was running in the foreground.
            override fun userNotificationCenter(
                center: UNUserNotificationCenter,
                willPresentNotification: UNNotification,
                withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
            ) {
                val userInfo = willPresentNotification.request.content.userInfo

                if (!userInfo.contains("skipForeground")) {
                    // Display alert, sound, or badge when the app is in the foreground
                    withCompletionHandler(
                        UNNotificationPresentationOptionAlert or UNNotificationPresentationOptionSound or UNNotificationPresentationOptionBadge
                    )
                }
            }
        }
        notificationDelegate = delegate
        UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
        logDebug("Notification center delegate applied")
    }

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
            )
        )
    }

    @Suppress("unused")  // Called from iosClient.swift
    fun setup() {
        setupDelegate()
        setupNotificationCategories()
    }
}

