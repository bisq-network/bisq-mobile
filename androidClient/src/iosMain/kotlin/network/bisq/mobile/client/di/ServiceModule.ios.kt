package network.bisq.mobile.client.di

import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.presentation.notification.ForegroundServiceController
import network.bisq.mobile.presentation.notification.ForegroundServiceControllerImpl
import network.bisq.mobile.presentation.notification.NotificationController
import network.bisq.mobile.presentation.notification.NotificationControllerImpl
import network.bisq.mobile.presentation.service.OpenTradesNotificationService
import org.koin.dsl.bind
import org.koin.dsl.module

actual val serviceModule = module {
    single<AppForegroundController> { AppForegroundController() } bind ForegroundDetector::class
    single<NotificationController> {
        NotificationControllerImpl()
    }
    single<ForegroundServiceController> {
        ForegroundServiceControllerImpl(get())
    }
    single<OpenTradesNotificationService> {
        OpenTradesNotificationService(get(), get(), get(), get())
    }
}