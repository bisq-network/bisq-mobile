package network.bisq.mobile.client.common.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.client.common.domain.access.pairing.PairingApiGateway
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.session.SessionApiGateway
import network.bisq.mobile.client.common.domain.access.session.SessionService
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryImpl
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsSerializer
import network.bisq.mobile.client.common.domain.service.accounts.ClientFiatAccountsServiceFacade
import network.bisq.mobile.client.common.domain.service.accounts.FiatPaymentAccountsApiGateway
import network.bisq.mobile.client.common.domain.service.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.common.domain.service.chat.trade.ClientTradeChatMessagesServiceFacade
import network.bisq.mobile.client.common.domain.service.chat.trade.TradeChatMessagesApiGateway
import network.bisq.mobile.client.common.domain.service.common.ClientLanguageServiceFacade
import network.bisq.mobile.client.common.domain.service.explorer.ClientExplorerServiceFacade
import network.bisq.mobile.client.common.domain.service.explorer.ExplorerApiGateway
import network.bisq.mobile.client.common.domain.service.market.ClientMarketPriceServiceFacade
import network.bisq.mobile.client.common.domain.service.market.MarketPriceApiGateway
import network.bisq.mobile.client.common.domain.service.mediation.ClientMediationServiceFacade
import network.bisq.mobile.client.common.domain.service.mediation.MediationApiGateway
import network.bisq.mobile.client.common.domain.service.message_delivery.ClientMessageDeliveryServiceFacade
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.client.common.domain.service.network.ClientNetworkServiceFacade
import network.bisq.mobile.client.common.domain.service.offers.ClientOffersServiceFacade
import network.bisq.mobile.client.common.domain.service.offers.OfferbookApiGateway
import network.bisq.mobile.client.common.domain.service.reputation.ClientReputationServiceFacade
import network.bisq.mobile.client.common.domain.service.reputation.ReputationApiGateway
import network.bisq.mobile.client.common.domain.service.settings.ClientSettingsServiceFacade
import network.bisq.mobile.client.common.domain.service.settings.SettingsApiGateway
import network.bisq.mobile.client.common.domain.service.trades.ClientTradesServiceFacade
import network.bisq.mobile.client.common.domain.service.trades.TradesApiGateway
import network.bisq.mobile.client.common.domain.service.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.client.common.domain.service.user_profile.UserProfileApiGateway
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientFactory
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.messages.SubscriptionRequest
import network.bisq.mobile.client.common.domain.websocket.messages.SubscriptionResponse
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.domain.data.EnvironmentController
import network.bisq.mobile.domain.data.datastore.createDataStore
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
import network.bisq.mobile.domain.getStorageDir
import network.bisq.mobile.domain.service.accounts.FiatAccountsServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.service.network.NetworkServiceFacade
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.service.trades.TradesServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import okio.Path.Companion.toPath
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

// networking and services dependencies
val clientDomainModule =
    module {
        val json =
            Json {
                prettyPrint = true
                encodeDefaults = true
                serializersModule =
                    SerializersModule {
                        polymorphic(MonetaryVO::class) {
                            subclass(CoinVO::class, CoinVO.serializer())
                            subclass(FiatVO::class, FiatVO.serializer())
                        }
                        polymorphic(PriceSpecVO::class) {
                            subclass(
                                FixPriceSpecVO::class,
                                FixPriceSpecVO.serializer(),
                            )
                            subclass(
                                FloatPriceSpecVO::class,
                                FloatPriceSpecVO.serializer(),
                            )
                            subclass(
                                MarketPriceSpecVO::class,
                                MarketPriceSpecVO.serializer(),
                            )
                        }
                        polymorphic(AmountSpecVO::class) {
                            subclass(
                                QuoteSideFixedAmountSpecVO::class,
                                QuoteSideFixedAmountSpecVO.serializer(),
                            )
                            subclass(
                                QuoteSideRangeAmountSpecVO::class,
                                QuoteSideRangeAmountSpecVO.serializer(),
                            )
                            subclass(
                                BaseSideFixedAmountSpecVO::class,
                                BaseSideFixedAmountSpecVO.serializer(),
                            )
                            subclass(
                                BaseSideRangeAmountSpecVO::class,
                                BaseSideRangeAmountSpecVO.serializer(),
                            )
                        }
                        polymorphic(OfferOptionVO::class) {
                            subclass(
                                ReputationOptionVO::class,
                                ReputationOptionVO.serializer(),
                            )
                            subclass(
                                TradeTermsOptionVO::class,
                                TradeTermsOptionVO.serializer(),
                            )
                        }
                        polymorphic(PaymentMethodSpecVO::class) {
                            subclass(
                                BitcoinPaymentMethodSpecVO::class,
                                BitcoinPaymentMethodSpecVO.serializer(),
                            )
                            subclass(
                                FiatPaymentMethodSpecVO::class,
                                FiatPaymentMethodSpecVO.serializer(),
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

        single<ApplicationBootstrapFacade> {
            ClientApplicationBootstrapFacade(
                get(),
                get(),
                get(),
                get(),
            )
        }

        single { EnvironmentController() }
        single(named("ApiHost")) { get<EnvironmentController>().getApiHost() }
        single(named("ApiPort")) { get<EnvironmentController>().getApiPort() }
        single(named("WebSocketApiHost")) { get<EnvironmentController>().getWebSocketHost() }
        single(named("WebSocketApiPort")) { get<EnvironmentController>().getWebSocketPort() }

        single {
            HttpClientService(
                get(),
                get(),
                get(),
                get(),
                get(named("ApiHost")),
                get(named("ApiPort")),
            )
        }

        single { WebSocketClientFactory(get()) }

        single {
            WebSocketClientService(
                get(named("WebSocketApiHost")),
                get(named("WebSocketApiPort")),
                get(),
                get(),
            )
        }

        single { PairingApiGateway(get()) }
        single { PairingService(get()) }
        single { SessionApiGateway(get()) }
        single { SessionService(get()) }
        single { ApiAccessService(get(), get(), get()) }

        // single { WebSocketHttpClient(get()) }
        single {
            println("Running on simulator: ${get<EnvironmentController>().isSimulator()}")
            WebSocketApiClient(
                get(),
                get(),
            )
        }

        single { ClientConnectivityService(get()) } bind ConnectivityService::class

        single<NetworkServiceFacade> {
            ClientNetworkServiceFacade(
                get(),
                get(),
                get(),
                get(),
            )
        }

        single { MarketPriceApiGateway(get(), get()) }
        single<MarketPriceServiceFacade> {
            ClientMarketPriceServiceFacade(
                get(),
                get(),
                get(),
            )
        }

        single { UserProfileApiGateway(get(), get()) }
        single {
            ClientUserProfileServiceFacade(
                get(),
                get(),
                get(),
                get(),
            )
        } bind UserProfileServiceFacade::class

        single { OfferbookApiGateway(get(), get()) }
        single<OffersServiceFacade> {
            ClientOffersServiceFacade(
                get(),
                get(),
                get(),
                get(),
            )
        }

        single { TradesApiGateway(get(), get()) }
        single<TradesServiceFacade> {
            ClientTradesServiceFacade(
                get(),
                get(),
                get(),
            )
        }

        single { TradeChatMessagesApiGateway(get(), get()) }
        single<TradeChatMessagesServiceFacade> {
            ClientTradeChatMessagesServiceFacade(
                get(),
                get(),
                get(),
                get(),
            )
        }

        single { ExplorerApiGateway(get()) }
        single<ExplorerServiceFacade> { ClientExplorerServiceFacade(get()) }

        single { MediationApiGateway(get()) }
        single<MediationServiceFacade> { ClientMediationServiceFacade(get()) }

        single { SettingsApiGateway(get()) }
        single<SettingsServiceFacade> { ClientSettingsServiceFacade(get()) }

        single { FiatPaymentAccountsApiGateway(get()) }
        single<FiatAccountsServiceFacade> { ClientFiatAccountsServiceFacade(get()) }

        single<LanguageServiceFacade> { ClientLanguageServiceFacade() }

        single { ReputationApiGateway(get(), get()) }
        single<ReputationServiceFacade> {
            ClientReputationServiceFacade(
                get(),
                get(),
            )
        }

        single<MessageDeliveryServiceFacade> { ClientMessageDeliveryServiceFacade() }

        single<KmpTorService> {
            // ClientApp doesn't have Bisq2's Tor library to enable network via control port,
            // so we start with network enabled (disableNetworkInitially = false)
            KmpTorService(getStorageDir().toPath(true), disableNetworkInitially = false)
        }

        single<SensitiveSettingsRepository> {
            SensitiveSettingsRepositoryImpl(
                get(named("SensitiveSettings")),
            )
        }
        single<DataStore<SensitiveSettings>>(named("SensitiveSettings")) {
            createDataStore(
                "SensitiveSettings",
                getStorageDir(),
                SensitiveSettingsSerializer,
                ReplaceFileCorruptionHandler { SensitiveSettings() },
            )
        }
    }
