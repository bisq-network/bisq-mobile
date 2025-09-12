package network.bisq.mobile.client.di

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import network.bisq.mobile.client.network.HttpClientService
import network.bisq.mobile.client.network.WebSocketClientFactory
import network.bisq.mobile.client.websocket.WebSocketClientService
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
import org.koin.core.qualifier.named
import org.koin.dsl.module

val networkModule = module {
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

    single { EnvironmentController() }
    single(named("ApiHost")) { get<EnvironmentController>().getApiHost() }
    single(named("ApiPort")) { get<EnvironmentController>().getApiPort() }
    single(named("WebsocketApiHost")) { get<EnvironmentController>().getWebSocketHost() }
    single(named("WebsocketApiPort")) { get<EnvironmentController>().getWebSocketPort() }

    single {
        HttpClientService(
            get(),
            get(),
            get(named("ApiHost")),
            get(named("ApiPort")),
        )
    }

    single { WebSocketClientFactory(get()) }

    single {
        WebSocketClientService(
            get(),
            get(),
            get(),
            get(named("WebsocketApiHost")),
            get(named("WebsocketApiPort")),
        )
    }

    // single { WebSocketHttpClient(get()) }
    single {
        println("Running on simulator: ${get<EnvironmentController>().isSimulator()}")
        WebSocketApiClient(
            get(),
            get(),
            get(),
        )
    }
}