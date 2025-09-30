package network.bisq.mobile.client.di

import network.bisq.mobile.client.ClientMainActivity
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.presentation.notification.ForegroundServiceController
import network.bisq.mobile.presentation.notification.ForegroundServiceControllerImpl
import network.bisq.mobile.presentation.notification.NotificationController
import network.bisq.mobile.presentation.notification.NotificationControllerImpl
import network.bisq.mobile.presentation.service.OpenTradesNotificationService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val serviceModule = module {
    single<AppForegroundController> { AppForegroundController(androidContext()) } bind ForegroundDetector::class
    single<NotificationControllerImpl> {
        NotificationControllerImpl(get(), ClientMainActivity::class.java)
    }
    single<NotificationController> {
        get<NotificationControllerImpl>()
    }
    single<ForegroundServiceController> {
        ForegroundServiceControllerImpl(get())
    }
    single<OpenTradesNotificationService> {
        OpenTradesNotificationService(get(), get(), get(), get())
    }
}