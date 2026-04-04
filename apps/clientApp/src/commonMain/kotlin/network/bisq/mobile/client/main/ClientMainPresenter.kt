package network.bisq.mobile.client.main

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.client.common.presentation.navigation.TrustedNodeSetup
import network.bisq.mobile.client.common.presentation.navigation.TrustedNodeSetupSettings
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.repository.TradeReadStateRepository
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
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
        listenForConnectivity()
        observeClientRevocation()
    }

    private fun listenForConnectivity() {
        connectivityService.startMonitoring()
    }

    @ExcludeFromCoverage
    private fun observeClientRevocation() {
        presenterScope.launch {
            connectivityService.clientRevoked.collect { revoked ->
                if (revoked) {
                    log.i { "Client credentials revoked — navigating to pairing screen" }
                    showSnackbar(
                        "mobile.connect.clientRevoked".i18n(),
                        type = SnackbarType.ERROR,
                    )
                    connectivityService.acknowledgeRevocation()
                    navigateTo(TrustedNodeSetup()) { builder ->
                        builder.popUpTo(NavRoute.TabContainer) { inclusive = true }
                    }
                }
            }
        }
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
