package network.bisq.mobile.client

import kotlinx.coroutines.withContext
import network.bisq.mobile.client.service.network.ClientConnectivityService
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.service.OpenTradesNotificationService
import network.bisq.mobile.presentation.ui.error.GenericErrorHandler

/**
 * Contains all the share code for each client. Each specific app might extend this class if needed.
 */
open class ClientMainPresenter(
    private val connectivityService: ClientConnectivityService,
    private val settingsServiceFacade: SettingsServiceFacade,
    tradesServiceFacade: TradesServiceFacade,
    userProfileServiceFacade: UserProfileServiceFacade,
    openTradesNotificationService: OpenTradesNotificationService,
    tradeReadStateRepository: TradeReadStateRepository,
    private val webSocketClientProvider: WebSocketClientProvider,
    urlLauncher: UrlLauncher
) : MainPresenter(
    tradesServiceFacade,
    userProfileServiceFacade,
    openTradesNotificationService,
    settingsServiceFacade,
    tradeReadStateRepository,
    urlLauncher,
) {

    private var lastConnectedStatus: Boolean? = null

    override fun onViewAttached() {
        super.onViewAttached()
        validateVersion()
        listenForConnectivity()
    }


    private fun listenForConnectivity() {
        connectivityService.startMonitoring()
        launchUI {
            webSocketClientProvider.get().webSocketClientStatus.collect {
                if (webSocketClientProvider.get().isConnected() && lastConnectedStatus != true) {
                    log.d { "connectivity status changed to $it - reconnecting services" }
                    reactivateServices()
                    lastConnectedStatus = true
                } else {
                    lastConnectedStatus = false
                }
            }
        }
    }

    private fun validateVersion() {
        launchUI {
            val isApiCompatible = withContext(IODispatcher) { settingsServiceFacade.isApiCompatible() }
            if (!isApiCompatible) {
                log.w { "configured trusted node doesn't have a compatible api version" }

                val trustedNodeVersion = withContext(IODispatcher) { settingsServiceFacade.getTrustedNodeVersion() }
                GenericErrorHandler.handleGenericError(
                    "Your configured trusted node is running Bisq version $trustedNodeVersion.\n" +
                            "Bisq Connect requires version ${BuildConfig.BISQ_API_VERSION} to run properly.\n"
                )
            } else {
                log.d { "trusted node is compatible, continue" }
            }
        }
    }

    override fun onResumeServices() {
        super.onResumeServices()
        connectivityService.startMonitoring()
    }

    override fun onPauseServices() {
        super.onPauseServices()
        connectivityService.stopMonitoring()
    }

    override fun isDevMode(): Boolean {
        return isDemo() || BuildConfig.IS_DEBUG
    }

    override fun isDemo(): Boolean = ApplicationBootstrapFacade.isDemo
}