package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.common.domain.service.push_notification.AndroidPushNotificationTokenProvider
import network.bisq.mobile.client.common.domain.service.push_notification.ClientPushNotificationServiceFacade
import network.bisq.mobile.client.common.domain.service.push_notification.PushNotificationApiGateway
import network.bisq.mobile.client.common.domain.service.push_notification.PushNotificationTokenProvider
import network.bisq.mobile.client.main.ClientMainActivity
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.push_notification.PushNotificationServiceFacade
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

        // Push notification services
        single<PushNotificationTokenProvider> { AndroidPushNotificationTokenProvider() }
        single { PushNotificationApiGateway(get()) }
        single<PushNotificationServiceFacade> {
            ClientPushNotificationServiceFacade(get(), get(), get())
        }
    }
