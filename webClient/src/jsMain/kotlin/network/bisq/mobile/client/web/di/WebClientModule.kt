package network.bisq.mobile.client.web.di

import network.bisq.mobile.client.web.services.WebClientConnectivityService
import network.bisq.mobile.client.web.services.WebNotificationService
import network.bisq.mobile.client.web.services.WebUrlLauncher
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.WebAppPresenter
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