package network.bisq.mobile.android.node.presentation

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

    override suspend fun hasConnectivity(): Boolean {
        return mainPresenter.isConnected()
    }

    /**
     * Use only for corner cases / temporary solutions whilst
     * investigating a real fix
     */
    override fun restartApp() {
        log.i { "User requested app restart from failed state - restarting application" }

        try {
            // Get the activity from the main presenter
            val activity = (mainPresenter as NodeMainPresenter).getActivity()
            val packageManager = activity.packageManager
            val packageName = activity.packageName

            // Create restart intent
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.let { restartIntent ->
                restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)

                // Start the app
                activity.startActivity(restartIntent)

                // Exit current process
                kotlin.system.exitProcess(0)
            } ?: run {
                log.e { "Could not create restart intent" }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to restart app" }
        }
    }
}
