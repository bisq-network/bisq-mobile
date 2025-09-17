package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.NodeMainActivity
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val serviceModule = module {
    single<AppForegroundController> { AppForegroundController(androidContext()) } bind ForegroundDetector::class
    single<NotificationServiceController> {
        NotificationServiceController(get(), NodeMainActivity::class.java)
    }
}