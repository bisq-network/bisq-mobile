package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.NodeMainActivity
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
    single { AppForegroundController(androidContext()) } bind ForegroundDetector::class
    single {
        NotificationControllerImpl(
            get(),
            NodeMainActivity::class.java
        )
    } bind NotificationController::class
    single { ForegroundServiceControllerImpl(get()) } bind ForegroundServiceController::class
    single {
        OpenTradesNotificationService(get(), get(), get(), get(), get())
    }
}