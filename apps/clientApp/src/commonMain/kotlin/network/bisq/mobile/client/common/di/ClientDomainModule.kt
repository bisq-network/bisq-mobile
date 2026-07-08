package network.bisq.mobile.client.common.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.client.common.domain.access.pairing.PairingApiGateway
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.access.session.SessionApiGateway
import network.bisq.mobile.client.common.domain.access.session.SessionService
import network.bisq.mobile.client.common.domain.analytics.KmpTorSocksPortProvider
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryImpl
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsSerializer
import network.bisq.mobile.client.common.domain.service.accounts.user_defined.ClientUserDefinedAccountsServiceFacade
import network.bisq.mobile.client.common.domain.service.accounts.user_defined.UserDefinedPaymentAccountsApiGateway
import network.bisq.mobile.client.common.domain.service.alert.AlertNotificationsApiGateway
import network.bisq.mobile.client.common.domain.service.alert.ClientAlertNotificationsServiceFacade
import network.bisq.mobile.client.common.domain.service.alert.ClientTradeRestrictingAlertServiceFacade
import network.bisq.mobile.client.common.domain.service.alert.TradeRestrictingAlertApiGateway
import network.bisq.mobile.client.common.domain.service.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.common.domain.service.capabilities.ClientBackendCapabilitiesService
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
import network.bisq.mobile.client.payment_accounts.data.service.ClientPaymentAccountsServiceFacade
import network.bisq.mobile.client.payment_accounts.data.service.PaymentAccountsApiGateway
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.client.trusted_node_setup.use_case.TrustedNodeSetupUseCase
import network.bisq.mobile.data.datastore.createDataStore
import network.bisq.mobile.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.data.replicated.common.monetary.MonetaryVO
import network.bisq.mobile.data.replicated.offer.amount.spec.AmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.BaseSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.BaseSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideRangeAmountSpecVO
import network.bisq.mobile.data.replicated.offer.options.OfferOptionVO
import network.bisq.mobile.data.replicated.offer.options.ReputationOptionVO
import network.bisq.mobile.data.replicated.offer.options.TradeTermsOptionVO
import network.bisq.mobile.data.replicated.offer.payment_method.BitcoinPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.FiatPaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.payment_method.PaymentMethodSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.MarketPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.chat.trade.TradeChatMessagesServiceFacade
import network.bisq.mobile.data.service.common.LanguageServiceFacade
import network.bisq.mobile.data.service.explorer.ExplorerServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.mediation.MediationServiceFacade
import network.bisq.mobile.data.service.message_delivery.MessageDeliveryServiceFacade
import network.bisq.mobile.data.service.network.ConnectivityService
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.EnvironmentController
import network.bisq.mobile.data.utils.getPlatformInfo
import network.bisq.mobile.data.utils.getStorageDir
import network.bisq.mobile.domain.analytics.AnalyticsBootstrapConfig
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.analytics.AnalyticsSettingsBaseline
import network.bisq.mobile.domain.analytics.AnalyticsSocksPortProvider
import network.bisq.mobile.domain.analytics.BufferedAnalyticsService
import network.bisq.mobile.domain.analytics.SentryAnalyticsService
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
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

        single {
            ClientApplicationBootstrapFacade(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        } bind ApplicationBootstrapFacade::class

        // Opt-in analytics (issue #525). Two locks gate emission:
        //  1. Dev-only build gate: `BuildConfig.ANALYTICS_DEV_ENABLED`. In RELEASE
        //     builds this is always true (forced at BuildConfig generation time);
        //     in DEBUG builds it reads `feature.analyticsDevEnabled` from
        //     gradle/local.properties (default false). Protects production
        //     GlitchTip from being polluted by developer test events while
        //     guaranteeing end users get a ready-to-flip release build.
        //  2. User-facing runtime gate: `SettingsRepository.analyticsEnabled`.
        //     This is THE switch a user controls from the Settings UI — default
        //     OFF. Checked on every emit; flipping it ON in a release build
        //     starts emission immediately, no rebuild required.
        //
        // We ALWAYS bind SentryAnalyticsService now (no R8 pruning) — the
        // tradeoff is a few hundred KB of Sentry-KMP linked into release builds,
        // but the SDK stays inert until both gates pass + Tor is ready. The
        // upside is end users don't need a special build to enable analytics.
        //
        // BufferedAnalyticsService wraps SentryAnalyticsService: events fired
        // before the Sentry SDK is ready (e.g. during Tor bootstrap) sit in an
        // in-memory bounded buffer and drain when the lifecycle service calls
        // onSentryReady(). Bound BOTH as AnalyticsService AND as
        // BufferedAnalyticsService (so ApplicationLifecycleService can call
        // onSentryReady() on the concrete type).
        //
        // SOCKS port source: kmp-tor. Suspends until the user pairs an onion
        // trusted node and `TrustedNodeSetupUseCase` starts Tor. On a LAN /
        // clearnet trusted node this never resolves — events sit in the buffer
        // and evict by FIFO with no clearnet leak.
        single<AnalyticsSocksPortProvider> { KmpTorSocksPortProvider(get()) }
        single {
            // Independent scope so the buffer's periodic flusher survives any
            // individual feature-scope cancellation. SupervisorJob so a single
            // failed enqueue doesn't kill the flusher. Reused for the
            // settings-derived analyticsEnabled StateFlow so its hot-share
            // lives exactly as long as the BufferedAnalyticsService instance.
            val analyticsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val settingsRepository = get<SettingsRepository>()
            // Hot StateFlow view of `Settings.analyticsEnabled`. Backed by
            // DataStore so a settings flip from any UI surface propagates
            // immediately to this `.value` read in the runtime gate.
            val analyticsEnabledFlow = settingsRepository.analyticsEnabledIn(analyticsScope)
            BufferedAnalyticsService(
                downstream =
                    SentryAnalyticsService(
                        nativeInitializer = get(),
                        // Two gates AND'd together:
                        //  1) Dev-only build flag — release builds const-fold
                        //     this to true at BuildConfig generation time.
                        //  2) User-settings toggle — flipped from Settings UI.
                        // Both must be true for the SDK to emit anything.
                        runtimeOptInProvider = {
                            BuildConfig.ANALYTICS_DEV_ENABLED && analyticsEnabledFlow.value
                        },
                    ),
                scope = analyticsScope,
            )
        }
        single<AnalyticsService> { get<BufferedAnalyticsService>() }
        // Per-platform analytics bootstrap config. The Connect app ships a
        // separate GlitchTip project for Android (id=3) and iOS (id=4); the
        // right DSN is selected at runtime from the platform. `IS_DEBUG`
        // splits debug-mode events (tagged "development") from release events
        // ("production"). Debug builds need both feature.analyticsDevEnabled=true
        // in local.properties AND the user-settings toggle ON to emit.
        single<AnalyticsBootstrapConfig> {
            val dsn =
                when (getPlatformInfo().type) {
                    PlatformType.ANDROID -> BuildConfig.ANALYTICS_DSN_ANDROID
                    PlatformType.IOS -> BuildConfig.ANALYTICS_DSN_IOS
                }
            AnalyticsBootstrapConfig(
                dsn = dsn,
                environment = if (BuildConfig.IS_DEBUG) "development" else "production",
                release = "bisq-connect@${
                    when (getPlatformInfo().type) {
                        PlatformType.ANDROID -> BuildConfig.ANDROID_APP_VERSION
                        PlatformType.IOS -> BuildConfig.IOS_APP_VERSION
                    }
                }",
                isDebug = BuildConfig.IS_DEBUG,
            )
        }

        // Settings baseline emitter — fires a snapshot of the user-controlled
        // settings (analytics, language, push, keep-connected) once per
        // process AFTER the user opts into analytics, called from
        // ApplicationLifecycleService.bootstrapAnalytics. Reuses the existing
        // BufferedAnalyticsService so events go through the same gates.
        single {
            AnalyticsSettingsBaseline(
                analyticsService = get<AnalyticsService>(),
                settingsRepository = get<SettingsRepository>(),
                settingsServiceFacade = get<SettingsServiceFacade>(),
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
                get(),
                get(),
                kmpTorService = get(),
            )
        }

        single { PairingApiGateway(get()) }
        single { PairingService(get()) }
        single { PairingQrCodeDecoder(get()) }
        single { SessionApiGateway(get()) }
        single { SessionService(get()) }
        single { ApiAccessService(get(), get(), get(), get()) }

        // single { WebSocketHttpClient(get()) }
        single {
            println("Running on simulator: ${get<EnvironmentController>().isSimulator()}")
            WebSocketApiClient(
                get(),
                get(),
            )
        }

        single { ClientConnectivityService(get()) } bind ConnectivityService::class

        single<BackendCapabilitiesService> {
            ClientBackendCapabilitiesService(get(), get())
        }

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
                get(),
            )
        }

        single { ExplorerApiGateway(get()) }
        single<ExplorerServiceFacade> { ClientExplorerServiceFacade(get()) }

        single { MediationApiGateway(get()) }
        single<MediationServiceFacade> { ClientMediationServiceFacade(get(), get()) }

        single { SettingsApiGateway(get()) }
        single<SettingsServiceFacade> { ClientSettingsServiceFacade(get(), get()) }

        single { UserDefinedPaymentAccountsApiGateway(get()) }
        single<UserDefinedAccountsServiceFacade> { ClientUserDefinedAccountsServiceFacade(get()) }

        single { PaymentAccountsApiGateway(get()) }
        single<PaymentAccountsServiceFacade> { ClientPaymentAccountsServiceFacade(get(), get()) }

        single<LanguageServiceFacade> { ClientLanguageServiceFacade() }

        single { ReputationApiGateway(get(), get()) }
        single<ReputationServiceFacade> {
            ClientReputationServiceFacade(
                get(),
                get(),
            )
        }

        single<MessageDeliveryServiceFacade> { ClientMessageDeliveryServiceFacade() }

        single { AlertNotificationsApiGateway(get(), get()) }
        single<AlertNotificationsServiceFacade> {
            ClientAlertNotificationsServiceFacade(
                get(),
                get(),
            )
        }

        single { TradeRestrictingAlertApiGateway(get()) }
        single<TradeRestrictingAlertServiceFacade> {
            ClientTradeRestrictingAlertServiceFacade(
                get(),
                get(),
            )
        }

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

        factory { TrustedNodeSetupUseCase(get(), get(), get(), get(), get(), get()) }
    }
