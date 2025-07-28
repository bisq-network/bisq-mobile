package network.bisq.mobile.client.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import network.bisq.mobile.client.service.accounts.AccountsApiGateway
import network.bisq.mobile.client.service.accounts.ClientAccountsServiceFacade
import network.bisq.mobile.client.service.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.service.chat.trade.ClientTradeChatMessagesServiceFacade
import network.bisq.mobile.client.service.chat.trade.TradeChatMessagesApiGateway
import network.bisq.mobile.client.service.common.ClientLanguageServiceFacade
import network.bisq.mobile.client.service.explorer.ClientExplorerServiceFacade
import network.bisq.mobile.client.service.explorer.ExplorerApiGateway
import network.bisq.mobile.client.service.market.ClientMarketPriceServiceFacade
import network.bisq.mobile.client.service.market.MarketPriceApiGateway
import network.bisq.mobile.client.service.mediation.ClientMediationServiceFacade
import network.bisq.mobile.client.service.mediation.MediationApiGateway
import network.bisq.mobile.client.service.network_stats.ClientProfileStatsServiceFacade
import network.bisq.mobile.client.service.network_stats.UserProfileStats
import network.bisq.mobile.client.service.reputation.ReputationApiGateway
import network.bisq.mobile.client.service.offers.ClientOffersServiceFacade
import network.bisq.mobile.client.service.offers.OfferbookApiGateway
import network.bisq.mobile.client.service.reputation.ClientReputationServiceFacade
import network.bisq.mobile.client.service.settings.ClientSettingsServiceFacade
import network.bisq.mobile.client.service.settings.SettingsApiGateway
import network.bisq.mobile.client.service.trades.ClientTradesServiceFacade
import network.bisq.mobile.client.service.trades.TradesApiGateway
import network.bisq.mobile.client.service.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.client.service.user_profile.UserProfileApiGateway
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.domain.data.EnvironmentController
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.BaseSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.options.OfferOptionVO
import network.bisq.mobile.domain.data.replicated.offer.options.ReputationOptionVO
import network.bisq.mobile.domain.data.replicated.offer.options.TradeTermsOptionVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.payment_method.PaymentMethodSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.service.TrustedNodeService
import network.bisq.mobile.domain.service.accounts.AccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.network_stats.ProfileStatsServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

// networking and services dependencies
val clientModule = module {
    val json = Json {
        prettyPrint = true
        serializersModule = SerializersModule {
            polymorphic(MonetaryVO::class) {
                subclass(CoinVO::class, CoinVO.serializer())
                subclass(FiatVO::class, FiatVO.serializer())
            }
            polymorphic(PriceSpecVO::class) {
                subclass(FixPriceSpecVO::class, FixPriceSpecVO.serializer())
                subclass(FloatPriceSpecVO::class, FloatPriceSpecVO.serializer())
                subclass(MarketPriceSpecVO::class, MarketPriceSpecVO.serializer())
            }
            polymorphic(AmountSpecVO::class) {
                subclass(QuoteSideFixedAmountSpecVO::class, QuoteSideFixedAmountSpecVO.serializer())
                subclass(QuoteSideRangeAmountSpecVO::class, QuoteSideRangeAmountSpecVO.serializer())
                subclass(BaseSideFixedAmountSpecVO::class, BaseSideFixedAmountSpecVO.serializer())
                subclass(BaseSideRangeAmountSpecVO::class, BaseSideRangeAmountSpecVO.serializer())
            }
            polymorphic(OfferOptionVO::class) {
                subclass(ReputationOptionVO::class, ReputationOptionVO.serializer())
                subclass(
                    TradeTermsOptionVO::class,
                    TradeTermsOptionVO.serializer()
                )
            }
            polymorphic(PaymentMethodSpecVO::class) {
                subclass(
                    BitcoinPaymentMethodSpecVO::class,
                    BitcoinPaymentMethodSpecVO.serializer()
                )
                subclass(
                    FiatPaymentMethodSpecVO::class,
                    FiatPaymentMethodSpecVO.serializer()
                )
            }

            polymorphic(WebSocketMessage::class) {
                subclass(WebSocketRestApiRequest::class)
                subclass(WebSocketRestApiResponse::class)
                subclass(SubscriptionRequest::class)
                subclass(SubscriptionResponse::class)
                subclass(WebSocketEvent::class)
            }
        }
        classDiscriminator = "type"
        ignoreUnknownKeys = true
    }

    single { json }

    single {
        HttpClient(CIO) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(json)
            }
        }
    }

    single<ApplicationBootstrapFacade> { ClientApplicationBootstrapFacade(get(), get()) }

    single { EnvironmentController() }
    single(named("ApiHost")) { get<EnvironmentController>().getApiHost() }
    single(named("ApiPort")) { get<EnvironmentController>().getApiPort() }
    single(named("WebsocketApiHost")) { get<EnvironmentController>().getWebSocketHost() }
    single(named("WebsocketApiPort")) { get<EnvironmentController>().getWebSocketPort() }

    factory { (host: String, port: Int) ->
        WebSocketClient(
            get(),
            get(),
            host,
            port
        )
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

    single { MarketPriceApiGateway(get(), get()) }
    single<MarketPriceServiceFacade> { ClientMarketPriceServiceFacade(get(), get(), get()) }

    single { UserProfileApiGateway(get()) }
    single<UserProfileServiceFacade> { ClientUserProfileServiceFacade(get(), get()) }

    single { OfferbookApiGateway(get(), get()) }
    single<OffersServiceFacade> { ClientOffersServiceFacade(get(), get(), get(), get()) }

    single { TradesApiGateway(get(), get()) }
    single<TradesServiceFacade> { ClientTradesServiceFacade(get(), get(), get(), get()) }

    single { TradeChatMessagesApiGateway(get(), get()) }
    single<TradeChatMessagesServiceFacade> { ClientTradeChatMessagesServiceFacade(get(), get(), get(), get()) }

    single { ExplorerApiGateway(get()) }
    single<ExplorerServiceFacade> { ClientExplorerServiceFacade(get()) }

    single { MediationApiGateway(get()) }
    single<MediationServiceFacade> { ClientMediationServiceFacade(get()) }

    single { SettingsApiGateway(get()) }
    single<SettingsServiceFacade> { ClientSettingsServiceFacade(get()) }

    single { AccountsApiGateway(get(), get()) }
    single<AccountsServiceFacade> { ClientAccountsServiceFacade(get()) }

    single<LanguageServiceFacade> { ClientLanguageServiceFacade() }

    single { ReputationApiGateway(get()) }
    single<ReputationServiceFacade> { ClientReputationServiceFacade(get(), get()) }

    single { UserProfileStats(get(), get()) }
    single<ProfileStatsServiceFacade> { ClientProfileStatsServiceFacade(get(), get()) }

}