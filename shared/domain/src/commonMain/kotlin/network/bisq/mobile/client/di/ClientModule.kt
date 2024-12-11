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
import network.bisq.mobile.android.node.main.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.market.ClientMarketPriceServiceFacade
import network.bisq.mobile.client.market.MarketPriceApiGateway
import network.bisq.mobile.client.offerbook.ClientOfferbookServiceFacade
import network.bisq.mobile.client.offerbook.offer.OfferbookApiGateway
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.websocket.messages.WebSocketRequest
import network.bisq.mobile.client.websocket.messages.WebSocketResponse
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.client.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.client.user_profile.UserProfileApiGateway
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.offerbook.OfferbookServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.utils.ByteArrayAsBase64Serializer
import org.koin.core.qualifier.named
import org.koin.dsl.module

// networking and services dependencies
val clientModule = module {
    val json = Json {
        serializersModule = SerializersModule {
            contextual(ByteArrayAsBase64Serializer)
            polymorphic(WebSocketMessage::class) {
                subclass(WebSocketEvent::class, WebSocketEvent.serializer())
                polymorphic(WebSocketRequest::class) {
                    subclass(WebSocketRestApiRequest::class, WebSocketRestApiRequest.serializer())
                    subclass(SubscriptionRequest::class, SubscriptionRequest.serializer())
                }
                polymorphic(WebSocketResponse::class) {
                    subclass(WebSocketRestApiResponse::class, WebSocketRestApiResponse.serializer())
                    subclass(SubscriptionResponse::class, SubscriptionResponse.serializer())
                }
            }
        }
        classDiscriminator = "className" // Default is "type" but we prefer more specific
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

    single<ApplicationBootstrapFacade> { ClientApplicationBootstrapFacade() }

    single(named("ApiHost")) { provideApiHost() }
    single(named("ApiPort")) { (BuildConfig.WS_PORT.takeIf { it.isNotEmpty() } ?: "8090").toInt() }
    single(named("WebsocketApiHost")) { provideWebsocketHost() }
    single(named("WebsocketApiPort")) { (BuildConfig.WS_PORT.takeIf { it.isNotEmpty() } ?: "8090").toInt() }

    single {
        WebSocketClient(
            get(),
            get(),
            get(named("WebsocketApiHost")),
            get(named("WebsocketApiPort"))
        )
    }
    // single { WebSocketHttpClient(get()) }
    single {
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
    single<OfferbookServiceFacade> { ClientOfferbookServiceFacade(get(), get(), get(), get()) }
}

fun provideApiHost(): String {
    return BuildConfig.WS_ANDROID_HOST.takeIf { it.isNotEmpty() } ?: "10.0.2.2"
}

fun provideWebsocketHost(): String {
    return BuildConfig.WS_ANDROID_HOST.takeIf { it.isNotEmpty() } ?: "10.0.2.2"
}