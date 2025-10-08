package network.bisq.mobile.client

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.domain.data.model.TorConfig
import network.bisq.mobile.domain.service.BaseService
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network.KmpTorClientService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade

class ClientApplicationLifecycleService(
    private val kmpTorClientService: KmpTorClientService,
    private val accountsServiceFacade: AccountsServiceFacade,
    private val applicationBootstrapFacade: ApplicationBootstrapFacade,
    private val tradeChatMessagesServiceFacade: TradeChatMessagesServiceFacade,
    private val languageServiceFacade: LanguageServiceFacade,
    private val explorerServiceFacade: ExplorerServiceFacade,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val mediationServiceFacade: MediationServiceFacade,
    private val offersServiceFacade: OffersServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val tradesServiceFacade: TradesServiceFacade,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val networkServiceFacade: NetworkServiceFacade,
    private val messageDeliveryServiceFacade: MessageDeliveryServiceFacade,
    private val connectivityService: ConnectivityService,
) : BaseService() {

    companion object Companion {
        const val TIMEOUT_SEC: Long = 60
    }

    private var baseDirPath: String? = null
    fun setBaseDirPath(value: String) {
        baseDirPath = value
    }

    fun initialize() {
        log.i { "Initialize core services and Tor" }

        launchIO {
            runCatching {
                networkServiceFacade.activate()
                applicationBootstrapFacade.activate()

                // TODO just temp, we should pass the tor dependency to applicationBootstrapFacade
                withContext(Dispatchers.Main) {
                    applicationBootstrapFacade.setProgress(0.1f)
                    applicationBootstrapFacade.setState("Starting Tor") // todo use i18n from node
                }
                if (TorConfig.useTor) {
                    // Block until tor is ready or a timeout exception is thrown
                    initializeTor().await()
                }

                // TODO just temp, we should pass the tor dependency to applicationBootstrapFacade
                withContext(Dispatchers.Main) {
                    applicationBootstrapFacade.setProgress(1f)
                    applicationBootstrapFacade.setState("Tor started") // todo use i18n from node
                }
                
                activateServiceFacades()
            }.onFailure { e ->
                log.e("Error at initializeTorAndServices", e)
                runCatching { networkServiceFacade.deactivate() }
                applicationBootstrapFacade.handleBootstrapFailure(e)
            }.also {
                // TODO implement the bootstrap in applicationBootstrapFacade and networkServiceFacade
                // ApplicationBootstrapFacade life cycle ends here in success and failure case.
                //applicationBootstrapFacade.deactivate()
            }
        }
    }

    fun shutdown() {
        log.i { "Destroying NodeMainPresenter" }
        shutdownServicesAndTor()
    }

    private fun shutdownServicesAndTor() {
        try {
            log.i { "Stopping service facades" }
            deactivateServiceFacades()
        } catch (e: Exception) {
            log.e("Error at deactivateServiceFacades", e)
        }

        try {
            log.i { "Stopping Tor" }
            kmpTorClientService.stopTorSync()
            log.i { "Tor stopped" }
        } catch (e: Exception) {
            log.e("Error at stopTor", e)
        }
    }

    private fun initializeTor(): CompletableDeferred<Boolean> {
        val result = CompletableDeferred<Boolean>()
        kmpTorClientService.setBaseDirPath(baseDirPath)
        launchIO {
            try {
                log.i { "Starting Tor" }
                // We block until Tor is ready, or timeout after 60 sec
                withTimeout(TIMEOUT_SEC * 1000) { kmpTorClientService.startTor().await() }
                log.i { "Tor successfully started" }
                result.complete(true)
            } catch (e: TimeoutCancellationException) {
                log.e(e) { "Tor initialization not completed after $TIMEOUT_SEC seconds" }
                result.completeExceptionally(e)
            } catch (e: CancellationException) {
                result.cancel(e)
                throw e
            } catch (e: Exception) {
                val failure = kmpTorClientService.startupFailure.value
                val errorMessage = listOfNotNull(
                    failure?.message,
                    failure?.cause?.message
                ).firstOrNull() ?: "Unknown Tor error"
                result.completeExceptionally(e)
                log.e(e) { "Tor initialization failed - $errorMessage" }
            }
        }
        return result
    }

    private fun activateServiceFacades() {
        settingsServiceFacade.activate()
        connectivityService.activate()
        offersServiceFacade.activate()
        marketPriceServiceFacade.activate()
        tradesServiceFacade.activate()
        tradeChatMessagesServiceFacade.activate()
        languageServiceFacade.activate()

        accountsServiceFacade.activate()
        explorerServiceFacade.activate()
        mediationServiceFacade.activate()
        reputationServiceFacade.activate()
        userProfileServiceFacade.activate()
        messageDeliveryServiceFacade.activate()
    }

    private fun deactivateServiceFacades() {
        connectivityService.deactivate()
        networkServiceFacade.deactivate()
        applicationBootstrapFacade.deactivate()
        settingsServiceFacade.deactivate()
        offersServiceFacade.deactivate()
        marketPriceServiceFacade.deactivate()
        tradesServiceFacade.deactivate()
        tradeChatMessagesServiceFacade.deactivate()
        languageServiceFacade.deactivate()

        accountsServiceFacade.deactivate()
        explorerServiceFacade.deactivate()
        mediationServiceFacade.deactivate()
        reputationServiceFacade.deactivate()
        userProfileServiceFacade.deactivate()
        messageDeliveryServiceFacade.deactivate()
    }
}
