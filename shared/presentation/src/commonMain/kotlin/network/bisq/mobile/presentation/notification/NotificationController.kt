package network.bisq.mobile.presentation.notification

import network.bisq.mobile.presentation.notification.model.NotificationBuilder
import network.bisq.mobile.presentation.notification.model.NotificationConfig

interface NotificationController {

    suspend fun hasPermission(): Boolean

    fun notify(builder: NotificationBuilder.() -> Unit) = notify(
        NotificationBuilder().apply(builder).build()
    )

    fun notify(config: NotificationConfig)

    fun cancel(id: String)

    fun isAppInForeground(): Boolean
}