package network.bisq.mobile.presentation.notification.model

import network.bisq.mobile.presentation.ui.navigation.DeepLinkableRoute

/**
 * Represents an action that will be taken when a NotificationButton is pressed
 *
 * @see NotificationButton
 */
sealed class NotificationPressAction {
    abstract val id: String

    data class Route(
        val route: DeepLinkableRoute,
        override val id: String = "route",
    ) : NotificationPressAction()

    class Default() : NotificationPressAction() {
        override val id: String = "default"

        override fun equals(other: Any?): Boolean {
            return other is Default
        }

        override fun hashCode(): Int {
            return "default".hashCode()
        }
    }
}
