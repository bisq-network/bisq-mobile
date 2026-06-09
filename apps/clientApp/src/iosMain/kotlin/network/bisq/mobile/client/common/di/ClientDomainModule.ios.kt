package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.common.domain.analytics.SentryCocoaNativeSentryInitializer
import network.bisq.mobile.client.common.domain.service.ClientApplicationLifecycleService
import network.bisq.mobile.client.common.domain.service.push_notification.ClientPushNotificationServiceFacade
import network.bisq.mobile.client.common.domain.service.push_notification.IosPushNotificationTokenProvider
import network.bisq.mobile.client.common.domain.service.push_notification.PushNotificationApiGateway
import network.bisq.mobile.client.common.domain.service.push_notification.PushNotificationTokenProvider
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.data.service.AppForegroundController
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.data.utils.IOSUrlLauncher
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.analytics.NativeSentryInitializer
import network.bisq.mobile.domain.utils.ClientVersionProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.ForegroundServiceControllerImpl
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationControllerImpl
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import org.koin.dsl.bind
import org.koin.dsl.module

val iosClientDomainModule =
    module {
        single { AppForegroundController() } bind ForegroundDetector::class
        single { NotificationControllerImpl(get()) } bind NotificationController::class
        single { ForegroundServiceControllerImpl(get()) } bind ForegroundServiceController::class
        single {
            OpenTradesNotificationService(get(), get(), get(), get(), get())
        }

        // Push notification services
        single<PushNotificationTokenProvider> { IosPushNotificationTokenProvider() }
        single { PushNotificationApiGateway(get()) }
        single<PushNotificationServiceFacade> {
            ClientPushNotificationServiceFacade(get(), get(), get(), get(), get())
        }

        single<ApplicationLifecycleService> {
            ClientApplicationLifecycleService(
                get(), // openTradesNotificationService
                get(), // kmpTorService
                get(), // fiatAccountsServiceFacade
                get(), // applicationBootstrapFacade
                get(), // tradeChatMessagesServiceFacade
                get(), // languageServiceFacade
                get(), // explorerServiceFacade
                get(), // marketPriceServiceFacade
                get(), // mediationServiceFacade
                get(), // offersServiceFacade
                get(), // reputationServiceFacade
                get(), // alertNotificationsServiceFacade
                get(), // tradeRestrictingAlertServiceFacade
                get(), // settingsServiceFacade
                get(), // tradesServiceFacade
                get(), // userProfileServiceFacade
                get(), // networkServiceFacade
                get(), // messageDeliveryServiceFacade
                get(), // connectivityService
                get(), // apiAccessService
                get(), // pushNotificationServiceFacade
                get(), // settingsRepository
                get(), // notificationController
                get(), // analyticsService
                get(), // analyticsBootstrapConfig
                getOrNull(), // bufferedAnalyticsService — only bound when ANALYTICS_ENABLED
                getOrNull(), // analyticsSocksPortProvider — only bound when ANALYTICS_ENABLED
            )
        }
        single<UrlLauncher> { IOSUrlLauncher() }
        single<VersionProvider> { ClientVersionProvider() }

        // Native Sentry SDK initializer — only bound when analytics is enabled
        // at build time. Unlike Android (where R8 prunes the Sentry-KMP classes
        // outright), iOS still statically links Sentry.framework via the pod()
        // declaration in clientApp/build.gradle.kts — but no Kotlin code paths
        // reference it when this binding is absent, so it stays inert.
        // TODO: gate the pod() declaration too (see clientApp/build.gradle.kts
        // for iOS pruning options).
        if (BuildConfig.ANALYTICS_ENABLED) {
            single<NativeSentryInitializer> { SentryCocoaNativeSentryInitializer() }
        }
    }
