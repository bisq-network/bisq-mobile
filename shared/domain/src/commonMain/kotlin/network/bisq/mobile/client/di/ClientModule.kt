package network.bisq.mobile.client.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import network.bisq.mobile.client.service.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.service.market.ClientMarketPriceServiceFacade
import network.bisq.mobile.client.service.market.MarketPriceApiGateway
import network.bisq.mobile.client.service.offer.ClientOfferServiceFacade
import network.bisq.mobile.client.service.offer.OfferApiGateway
import network.bisq.mobile.client.service.offerbook.ClientOfferbookServiceFacade
import network.bisq.mobile.client.service.offerbook.offer.OfferbookApiGateway
import network.bisq.mobile.client.service.trade.ClientTradeServiceFacade
import network.bisq.mobile.client.service.trade.TradeApiGateway
import network.bisq.mobile.client.service.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.client.service.user_profile.UserProfileApiGateway
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.domain.data.EnvironmentController
import network.bisq.mobile.domain.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.BaseSideRangeAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.domain.replicated.offer.options.OfferOptionVO
import network.bisq.mobile.domain.replicated.offer.options.ReputationOptionVO
import network.bisq.mobile.domain.replicated.offer.options.TradeTermsOptionVO
import network.bisq.mobile.domain.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.domain.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.domain.service.TrustedNodeService
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offer.OfferServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.domain.service.trade.TradeServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.utils.ByteArrayAsBase64Serializer
import org.koin.core.qualifier.named
import org.koin.dsl.module

// networking and services dependencies
val clientModule = module {
    val json = Json {
        prettyPrint = true
        serializersModule = SerializersModule {
            contextual(ByteArrayAsBase64Serializer)

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
                subclass(TradeTermsOptionVO::class, TradeTermsOptionVO.serializer())
            }
            polymorphic(network.bisq.mobile.domain.replicated.offer.payment_method.PaymentMethodSpecVO::class) {
                subclass(BitcoinPaymentMethodSpecVO::class, BitcoinPaymentMethodSpecVO.serializer())
                subclass(
                    network.bisq.mobile.domain.replicated.offer.payment_method.FiatPaymentMethodSpecVO::class,
                    network.bisq.mobile.domain.replicated.offer.payment_method.FiatPaymentMethodSpecVO.serializer()
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

    single {
        WebSocketClient(
            get(),
            get(),
            get(named("WebsocketApiHost")),
            get(named("WebsocketApiPort"))
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
    single<MarketPriceServiceFacade> { ClientMarketPriceServiceFacade(get(), get()) }

    single { UserProfileApiGateway(get()) }
    single<UserProfileServiceFacade> { ClientUserProfileServiceFacade(get(), get()) }

    single { OfferbookApiGateway(get(), get()) }
    single<OfferbookServiceFacade> { ClientOfferbookServiceFacade(get(), get(), get()) }

    single { OfferApiGateway(get(), get()) }
    single<OfferServiceFacade> { ClientOfferServiceFacade(get()) }

    single { TradeApiGateway(get(), get()) }
    single<TradeServiceFacade> { ClientTradeServiceFacade(get()) }

}