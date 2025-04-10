package network.bisq.mobile.client.web.di

import network.bisq.mobile.client.web.services.WebClientConnectivityService
import network.bisq.mobile.client.web.services.WebNotificationService
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.WebUrlLauncher
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.WebAppPresenter

import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

val webClientModule = module {
    // Web-specific implementations
    single<UrlLauncher> { WebUrlLauncher() }
    single<ClientConnectivityService> { WebClientConnectivityService(get()) }
    single<OpenTradesNotificationService> { WebNotificationService(get(), get()) }

    single<MainPresenter> {
        WebAppPresenter(
            get(), // ClientConnectivityService
            get(), // OpenTradesNotificationService
            get(), // TradesServiceFacade
            get(), // TradesServiceFacade
            get(), // TradeChatServiceFacade
            get(), // WebSocketClientProvider
            get(), // ApplicationBootstrapFacade
            get(), // OffersServiceFacade
            get(), // MarketPriceServiceFacade
            get(), // SettingsServiceFacade
            get(), // LanguageServiceFacade
            get()  // UrlLauncher
        )
    } bind AppPresenter::class
}