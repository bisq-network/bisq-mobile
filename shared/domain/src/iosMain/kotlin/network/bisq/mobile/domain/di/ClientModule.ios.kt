package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.IOSUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import org.koin.dsl.bind
import org.koin.dsl.module

val iosClientModule = module {
    single<UrlLauncher> { IOSUrlLauncher() }
    single<AppForegroundController> { AppForegroundController() } bind ForegroundDetector::class
    single<NotificationServiceController> {
        NotificationServiceController(get()).apply {
            this.registerBackgroundTask()
        }
    }

    single { ClientConnectivityService(get()) } bind ConnectivityService::class
}