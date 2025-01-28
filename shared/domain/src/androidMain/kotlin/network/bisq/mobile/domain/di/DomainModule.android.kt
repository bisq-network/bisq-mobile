package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind

val serviceModule = module {
    single<AppForegroundController> { AppForegroundController(androidContext()) } bind ForegroundDetector::class
    single<NotificationServiceController> { NotificationServiceController(get()) }
}