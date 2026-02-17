package network.bisq.mobile.client.main

import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.client.common.presentation.navigation.TrustedNodeSetupSettings
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.main.MainPresenter

/**
 * Contains all the share code for each client. Each specific app might extend this class if needed.
 */
open class ClientMainPresenter(
    private val connectivityService: ClientConnectivityService,
    private val networkServiceFacade: NetworkServiceFacade,
    settingsServiceFacade: SettingsServiceFacade,
    tradesServiceFacade: TradesServiceFacade,
    userProfileServiceFacade: UserProfileServiceFacade,
    openTradesNotificationService: OpenTradesNotificationService,
    tradeReadStateRepository: TradeReadStateRepository,
    applicationLifecycleService: ApplicationLifecycleService,
    urlLauncher: UrlLauncher,
) : MainPresenter(
        tradesServiceFacade,
        userProfileServiceFacade,
        openTradesNotificationService,
        settingsServiceFacade,
        tradeReadStateRepository,
        urlLauncher,
        applicationLifecycleService,
    ) {
    override fun onViewAttached() {
        super.onViewAttached()
//        activateServices()
//        validateVersion()
        listenForConnectivity()
    }

    private fun listenForConnectivity() {
        connectivityService.startMonitoring()
    }

    override fun onResume() {
        super.onResume()
        presenterScope.launch {
            runCatching {
                networkServiceFacade.ensureTorRunning()
            }.onFailure { exception ->
                log.e("Failed to ensure Tor is running", exception)
            }
        }
        connectivityService.startMonitoring()
    }

    override fun onPause() {
        super.onPause()
        connectivityService.stopMonitoring()
    }

    override fun isDevMode(): Boolean = isDemo() || BuildConfig.IS_DEBUG

    override fun isDemo(): Boolean = ApplicationBootstrapFacade.isDemo

    fun navigateToTrustedNode() {
        navigateTo(TrustedNodeSetupSettings)
    }
}
