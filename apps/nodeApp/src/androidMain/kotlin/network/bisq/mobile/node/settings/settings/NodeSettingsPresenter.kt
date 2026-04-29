package network.bisq.mobile.node.settings.settings

import network.bisq.mobile.data.service.common.LanguageServiceFacade
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.settings.settings.SettingsPresenter

class NodeSettingsPresenter(
    settingsServiceFacade: SettingsServiceFacade,
    languageServiceFacade: LanguageServiceFacade,
    pushNotificationServiceFacade: PushNotificationServiceFacade,
    mainPresenter: MainPresenter,
) : SettingsPresenter(
        settingsServiceFacade,
        languageServiceFacade,
        pushNotificationServiceFacade,
        mainPresenter,
    ) {
    override val shouldShowPoWAdjustmentFactor: Boolean
        get() = true

    /**
     * Hidden on Node — the embedded Bisq2 process posts notifications via the
     * local foreground service. FCM/APNs would be redundant and can't deliver
     * when the app is killed since the node would be killed too.
     */
    override val shouldShowPushNotificationsToggle: Boolean
        get() = false
}
