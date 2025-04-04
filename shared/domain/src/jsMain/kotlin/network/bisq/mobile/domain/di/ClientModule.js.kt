package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.JSUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import org.koin.dsl.bind
import org.koin.dsl.module

val jsClientModule = module {
    single<UrlLauncher> { JSUrlLauncher() }
    single { AppForegroundController() } bind ForegroundDetector::class
    single<NotificationServiceController> {
        NotificationServiceController(get())
    }

    single { ClientConnectivityService(get()) } bind ConnectivityService::class
}