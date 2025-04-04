package network.bisq.mobile.client.di

import io.ktor.client.*
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.service.accounts.AccountsApiGateway
import network.bisq.mobile.client.service.accounts.ClientAccountsServiceFacade
import network.bisq.mobile.client.service.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.service.chat.trade.ClientTradeChatMessagesServiceFacade
import network.bisq.mobile.client.service.chat.trade.TradeChatMessagesApiGateway
import network.bisq.mobile.client.service.chat.trade.ClientTradeChatServiceFacade
import network.bisq.mobile.client.service.common.ClientLanguageServiceFacade
import network.bisq.mobile.client.service.explorer.ClientExplorerServiceFacade
import network.bisq.mobile.client.service.explorer.ExplorerApiGateway
import network.bisq.mobile.client.service.market.ClientMarketPriceServiceFacade
import network.bisq.mobile.client.service.market.MarketPriceApiGateway
import network.bisq.mobile.client.service.mediation.ClientMediationServiceFacade
import network.bisq.mobile.client.service.mediation.MediationApiGateway
import network.bisq.mobile.client.service.mediation.ReputationApiGateway
import network.bisq.mobile.client.service.offers.ClientOffersServiceFacade
import network.bisq.mobile.client.service.offers.OfferbookApiGateway
import network.bisq.mobile.client.service.reputation.ClientReputationServiceFacade
import network.bisq.mobile.client.service.settings.ClientSettingsServiceFacade
import network.bisq.mobile.client.service.settings.SettingsApiGateway
import network.bisq.mobile.client.service.trades.ClientTradesServiceFacade
import network.bisq.mobile.client.service.trades.TradesApiGateway
import network.bisq.mobile.client.service.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.client.service.user_profile.UserProfileApiGateway
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.data.EnvironmentController
import network.bisq.mobile.domain.service.TrustedNodeService
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import network.bisq.mobile.domain.createHttpClient
import network.bisq.mobile.domain.service.chat.trade.TradeChatServiceFacade
import org.koin.dsl.module

val clientModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }
    }


    single { EnvironmentController() }
    single(named("ApiHost")) { get<EnvironmentController>().getApiHost() }
    single(named("ApiPort")) { get<EnvironmentController>().getApiPort() }
    single(named("WebsocketApiHost")) { get<EnvironmentController>().getWebSocketHost() }
    single(named("WebsocketApiPort")) { get<EnvironmentController>().getWebSocketPort() }

    single {
        createHttpClient(get())
    }

    single {
        WebSocketClientProvider(
            get(named("WebsocketApiHost")),
            get(named("WebsocketApiPort")),
            get(),
            clientFactory = { host, port -> get { parametersOf(host, port) } },
        )
    }

    single { TrustedNodeService(get()) }

    // single { WebSocketHttpClient(get()) }
    single {
        println("Running on simulator: ${get<EnvironmentController>().isSimulator()}")
        WebSocketApiClient(
            get(),
            get(),
            get(),
            get(named("WebsocketApiHost")),
            get(named("WebsocketApiPort"))
        )
    }

    single<LanguageServiceFacade> { ClientLanguageServiceFacade(get()) }

    single { MarketPriceApiGateway(get(), get()) }
    single<MarketPriceServiceFacade> { ClientMarketPriceServiceFacade(get(), get()) }

    single { UserProfileApiGateway(get()) }
    single<UserProfileServiceFacade> { ClientUserProfileServiceFacade(get(), get()) }

    single { OfferbookApiGateway(get(), get()) }
    single<OffersServiceFacade> { ClientOffersServiceFacade(get(), get(), get()) }

    single { TradesApiGateway(get(), get()) }
    single<TradesServiceFacade> { ClientTradesServiceFacade(get(), get(), get()) }

    single { TradeChatMessagesApiGateway(get(), get()) }
    single<TradeChatMessagesServiceFacade> { ClientTradeChatMessagesServiceFacade(get(), get(), get(), get()) }

    single { ExplorerApiGateway(get()) }
    single<ExplorerServiceFacade> { ClientExplorerServiceFacade(get()) }

    single { MediationApiGateway(get()) }
    single<MediationServiceFacade> { ClientMediationServiceFacade(get()) }

    single { SettingsApiGateway(get()) }
    single<SettingsServiceFacade> { ClientSettingsServiceFacade(get()) }

    single { AccountsApiGateway(get(), get()) }
    single<AccountsServiceFacade> { ClientAccountsServiceFacade(get(), get()) }

    single<LanguageServiceFacade> { ClientLanguageServiceFacade(get()) }

    single { ReputationApiGateway(get()) }
    single<ReputationServiceFacade> { ClientReputationServiceFacade(get()) }

}