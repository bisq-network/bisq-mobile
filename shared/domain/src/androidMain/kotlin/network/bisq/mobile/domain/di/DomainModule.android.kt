package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext

val serviceModule = module {
    single<NotificationServiceController> { NotificationServiceController(androidContext()) }
}