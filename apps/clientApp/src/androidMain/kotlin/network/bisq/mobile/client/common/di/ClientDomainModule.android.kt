package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.common.domain.service.push_notification.AndroidPushNotificationTokenProvider
import network.bisq.mobile.client.common.domain.service.push_notification.ClientPushNotificationServiceFacade
import network.bisq.mobile.client.common.domain.service.push_notification.PushNotificationApiGateway
import network.bisq.mobile.client.common.domain.service.push_notification.PushNotificationTokenProvider
import network.bisq.mobile.client.main.ClientMainActivity
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.data.service.AppForegroundController
import network.bisq.mobile.data.service.ForegroundDetector
import network.bisq.mobile.data.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.domain.analytics.NativeSentryInitializer
import network.bisq.mobile.domain.analytics.SentryJavaNativeSentryInitializer
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.ForegroundServiceControllerImpl
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationControllerImpl
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidClientDomainModule =
    module {
        single { AppForegroundController(androidContext()) } bind ForegroundDetector::class
        single {
            NotificationControllerImpl(
                get(),
                ClientMainActivity::class.java,
            )
        } bind NotificationController::class
        single { ForegroundServiceControllerImpl(get()) } bind ForegroundServiceController::class
        single {
            OpenTradesNotificationService(get(), get(), get(), get(), get())
        }

        // Push notification services — FCM-backed (auto-init OFF until user opts in,
        // see AndroidManifest.xml meta-data + AndroidPushNotificationTokenProvider).
        single<PushNotificationTokenProvider> { AndroidPushNotificationTokenProvider() }
        single { PushNotificationApiGateway(get()) }
        single<PushNotificationServiceFacade> {
            ClientPushNotificationServiceFacade(get(), get(), get(), get(), get())
        }

        // Native Sentry SDK initializer — only bound when analytics is enabled
        // at build time. The factory itself uses sentry-android-core types, so
        // gating the binding lets R8 prune SentryJavaNativeSentryInitializer
        // (and the Sentry-KMP SDK it touches) from analytics-disabled release
        // builds. Verified empirically by grepping the release APK for the
        // class name — see [[project_analytics_phase0_plan]].
        if (BuildConfig.ANALYTICS_ENABLED) {
            single<NativeSentryInitializer> { SentryJavaNativeSentryInitializer() }
        }
    }
