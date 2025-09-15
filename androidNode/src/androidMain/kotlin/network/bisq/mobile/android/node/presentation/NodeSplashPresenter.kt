package network.bisq.mobile.android.node.presentation

import android.app.Activity
import network.bisq.mobile.android.node.NodeApplicationLifecycleController
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter

class NodeSplashPresenter(
    private val mainPresenter: MainPresenter,
    applicationBootstrapFacade: ApplicationBootstrapFacade,
    userProfileService: UserProfileServiceFacade,
    userRepository: UserRepository,
    settingsRepository: SettingsRepository,
    settingsServiceFacade: SettingsServiceFacade,
    languageServiceFacade: LanguageServiceFacade,
    private val nodeApplicationLifecycleController: NodeApplicationLifecycleController
) : SplashPresenter(
    mainPresenter,
    applicationBootstrapFacade,
    userProfileService,
    userRepository,
    settingsRepository,
    settingsServiceFacade,
    languageServiceFacade,
    null
) {

    override fun doCustomNavigationLogic(settings: Settings, hasProfile: Boolean): Boolean {
        navigateToCreateProfile()
        // do nothing
        return false
    }

    override suspend fun isClientAndHasNoConnectivity(): Boolean {
        return false
    }

    // TODO refactor not not need that
    override suspend fun handleDemoModeForClient() {
        // Do nothing, only used in client mode
    }

    override suspend fun hasConnectivity(): Boolean {
        return mainPresenter.isConnected()
    }

    // TODO Would be better if all such code is extracted in a ClientSplashPresenter making such guards not needed
    override fun navigateToTrustedNodeSetup() {
        log.w { "navigateToTrustedNodeSetup called on node app. This should never happen." }
        // Ensure we never call TrustedNodeSetup in node mode
    }

    /**
     * Use only for corner cases / temporary solutions whilst
     * investigating a real fix
     */
    override fun restartApp() {
        log.i { "User requested app restart from failed state - restarting application" }

        nodeApplicationLifecycleController.restartApp(view as Activity)
    }

    override fun shutdownApp() {
        log.i { "User requested app shutdown from failed state" }

        nodeApplicationLifecycleController.shutdownApp()
    }
}
