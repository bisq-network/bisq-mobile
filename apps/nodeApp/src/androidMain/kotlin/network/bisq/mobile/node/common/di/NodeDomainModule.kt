package network.bisq.mobile.node.common.di

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.data.service.AppForegroundController
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.accounts.UserDefinedAccountsServiceFacade
import network.bisq.mobile.data.service.alert.AlertNotificationsServiceFacade
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
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
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.data.service.reputation.ReputationServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.data.utils.AndroidUrlLauncher
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.analytics.AnalyticsBootstrapConfig
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.analytics.AnalyticsSettingsBaseline
import network.bisq.mobile.domain.analytics.AnalyticsSocksPortProvider
import network.bisq.mobile.domain.analytics.BufferedAnalyticsService
import network.bisq.mobile.domain.analytics.NativeSentryInitializer
import network.bisq.mobile.domain.analytics.SentryAnalyticsService
import network.bisq.mobile.domain.analytics.SentryJavaNativeSentryInitializer
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.service.capabilities.BackendCapabilitiesService
import network.bisq.mobile.domain.utils.AndroidDeviceInfoProvider
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.node.BuildConfig
import network.bisq.mobile.node.common.domain.analytics.Bisq2SocksPortProvider
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.domain.service.NodeApplicationLifecycleService
import network.bisq.mobile.node.common.domain.service.accounts.NodeUserDefinedAccountsServiceFacade
import network.bisq.mobile.node.common.domain.service.alert.NodeAlertNotificationsServiceFacade
import network.bisq.mobile.node.common.domain.service.alert.NodeTradeRestrictingAlertServiceFacade
import network.bisq.mobile.node.common.domain.service.bootstrap.NodeApplicationBootstrapFacade
import network.bisq.mobile.node.common.domain.service.capabilities.NodeBackendCapabilitiesService
import network.bisq.mobile.node.common.domain.service.cat_hash.AndroidNodeCatHashService
import network.bisq.mobile.node.common.domain.service.chat.trade.NodeTradeChatMessagesServiceFacade
import network.bisq.mobile.node.common.domain.service.common.NodeLanguageServiceFacade
import network.bisq.mobile.node.common.domain.service.explorer.NodeExplorerServiceFacade
import network.bisq.mobile.node.common.domain.service.market_price.NodeMarketPriceServiceFacade
import network.bisq.mobile.node.common.domain.service.mediation.NodeMediationServiceFacade
import network.bisq.mobile.node.common.domain.service.message_delivery.NodeMessageDeliveryServiceFacade
import network.bisq.mobile.node.common.domain.service.network.NodeConnectivityService
import network.bisq.mobile.node.common.domain.service.network.NodeNetworkServiceFacade
import network.bisq.mobile.node.common.domain.service.offers.NodeOffersServiceFacade
import network.bisq.mobile.node.common.domain.service.push_notification.NoOpPushNotificationServiceFacade
import network.bisq.mobile.node.common.domain.service.reputation.NodeReputationServiceFacade
import network.bisq.mobile.node.common.domain.service.settings.NodeSettingsServiceFacade
import network.bisq.mobile.node.common.domain.service.trades.NodeTradesServiceFacade
import network.bisq.mobile.node.common.domain.service.user_profile.NodeUserProfileServiceFacade
import network.bisq.mobile.node.common.domain.utils.AndroidMemoryReportService
import network.bisq.mobile.node.common.domain.utils.NodeVersionProvider
import network.bisq.mobile.node.main.NodeMainActivity
import network.bisq.mobile.node.settings.backup.domain.NodeBackupServiceFacade
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.ForegroundServiceControllerImpl
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationControllerImpl
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodeDomainModule =
    module {
        // System services for memory reporting
        single<ActivityManager> { androidContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
        single { ActivityManager.MemoryInfo() }
        single { Debug.MemoryInfo() }
        single { Runtime.getRuntime() }

        single<AndroidMemoryReportService> {
            val isDebug = BuildConfig.DEBUG
            AndroidMemoryReportService(get(), get(), get(), get(), isDebug)
        }

        single<AndroidNodeCatHashService> {
            val context = androidContext()
            AndroidNodeCatHashService(context, context.filesDir.toPath())
        }

        single<AndroidApplicationService> {
            AndroidApplicationService(get(), androidContext(), androidContext().filesDir.toPath())
        }

        single {
            val provider = AndroidApplicationService.Provider()
            provider.applicationService = get<AndroidApplicationService>()
            provider
        }

        single<MessageDeliveryServiceFacade> { NodeMessageDeliveryServiceFacade(get()) }

        single { NodeNetworkServiceFacade(get(), get()) } bind NetworkServiceFacade::class

        single<KmpTorService> {
            val applicationService = get<AndroidApplicationService>()
            KmpTorService(applicationService.config.appDataDirPath.toOkioPath(true))
        }

        single { NodeApplicationBootstrapFacade(get(), get()) } bind ApplicationBootstrapFacade::class

        // Opt-in analytics (issue #525). Same shape as clientApp's binding —
        // see ClientDomainModule for the full double-lock rationale (dev gate
        // + user-settings gate) and the BufferedAnalyticsService double-binding
        // pattern. Node app sends to the bisq-easy-node-android GlitchTip
        // project (DSN from gradle.properties / local.properties).
        single<NativeSentryInitializer> { SentryJavaNativeSentryInitializer() }
        // SOCKS port source: bisq2's embedded NetworkService. The node app's
        // KmpTorService binding is NEVER started (its startTor() is only
        // called from the Connect-side TrustedNodeSetupUseCase) — so we must
        // NOT wire KmpTorSocksPortProvider here or analytics would suspend
        // forever waiting on a Tor instance nobody ever starts.
        single<AnalyticsSocksPortProvider> { Bisq2SocksPortProvider(get()) }
        single {
            // See ClientDomainModule for the matching pattern + rationale.
            val analyticsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val settingsRepository = get<SettingsRepository>()
            val analyticsEnabledFlow = settingsRepository.analyticsEnabledIn(analyticsScope)
            BufferedAnalyticsService(
                downstream =
                    SentryAnalyticsService(
                        nativeInitializer = get(),
                        runtimeOptInProvider = {
                            BuildNodeConfig.ANALYTICS_DEV_ENABLED && analyticsEnabledFlow.value
                        },
                    ),
                scope = analyticsScope,
            )
        }
        single<AnalyticsService> { get<BufferedAnalyticsService>() }
        single<AnalyticsBootstrapConfig> {
            AnalyticsBootstrapConfig(
                dsn = BuildNodeConfig.ANALYTICS_DSN,
                environment = if (BuildNodeConfig.IS_DEBUG) "development" else "production",
                release = "bisq-easy-node@${BuildNodeConfig.APP_VERSION}",
                isDebug = BuildNodeConfig.IS_DEBUG,
            )
        }

        // Settings baseline emitter — fires a snapshot of the user-controlled
        // settings (analytics, language, push, keep-connected) once per
        // process AFTER the user opts into analytics, called from
        // ApplicationLifecycleService.bootstrapAnalytics. Reuses the
        // BufferedAnalyticsService so events go through the same gates.
        single {
            AnalyticsSettingsBaseline(
                analyticsService = get<AnalyticsService>(),
                settingsRepository = get<SettingsRepository>(),
                settingsServiceFacade = get<SettingsServiceFacade>(),
            )
        }

        single<MarketPriceServiceFacade> { NodeMarketPriceServiceFacade(get(), get()) }

        single<UserProfileServiceFacade> { NodeUserProfileServiceFacade(get()) }

        single<OffersServiceFacade> { NodeOffersServiceFacade(get(), get(), get(), get()) }

        single<ExplorerServiceFacade> { NodeExplorerServiceFacade(get()) }

        single<TradesServiceFacade> { NodeTradesServiceFacade(get()) }

        single<TradeChatMessagesServiceFacade> {
            NodeTradeChatMessagesServiceFacade(
                get(),
                get(),
                get(),
            )
        }

        single<MediationServiceFacade> { NodeMediationServiceFacade(get()) }

        single<AlertNotificationsServiceFacade> { NodeAlertNotificationsServiceFacade(get()) }

        single<TradeRestrictingAlertServiceFacade> { NodeTradeRestrictingAlertServiceFacade(get()) }

        single<SettingsServiceFacade> { NodeSettingsServiceFacade(get()) }

        single<UserDefinedAccountsServiceFacade> { NodeUserDefinedAccountsServiceFacade(get()) }

        single<LanguageServiceFacade> { NodeLanguageServiceFacade() }

        single<NodeBackupServiceFacade> { NodeBackupServiceFacade(get(), get()) }

        single<ReputationServiceFacade> { NodeReputationServiceFacade(get()) }

        single { NodeConnectivityService(get()) } bind ConnectivityService::class

        single<BackendCapabilitiesService> { NodeBackendCapabilitiesService() }

        single<UrlLauncher> { AndroidUrlLauncher(androidContext()) }

        single<NodeApplicationLifecycleService> {
            NodeApplicationLifecycleService(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(), // analyticsService
                get(), // analyticsBootstrapConfig
                getOrNull(), // bufferedAnalyticsService — always bound now (dev + user-settings gates at runtime)
                getOrNull(), // analyticsSocksPortProvider — always bound now (dev + user-settings gates at runtime)
                get<SettingsRepository>(), // pre-warm DataStore before flipping onSentryReady — see ApplicationLifecycleService
                getOrNull(), // analyticsSettingsBaseline — always bound (post-opt-in baseline emitter)
            )
        } bind ApplicationLifecycleService::class

        single<DeviceInfoProvider> { AndroidDeviceInfoProvider(androidContext()) }

        single<VersionProvider> { NodeVersionProvider() }

        single { AppForegroundController(androidContext()) } bind ForegroundDetector::class

        single {
            NotificationControllerImpl(
                get(),
                NodeMainActivity::class.java,
            )
        } bind NotificationController::class

        single { ForegroundServiceControllerImpl(get()) } bind ForegroundServiceController::class

        single {
            OpenTradesNotificationService(get(), get(), get(), get(), get())
        }

        // Push notification service - no-op for node app
        single<PushNotificationServiceFacade> {
            NoOpPushNotificationServiceFacade()
        }
    }
