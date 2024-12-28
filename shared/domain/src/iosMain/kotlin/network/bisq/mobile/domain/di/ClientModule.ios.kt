package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.IOSUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import org.koin.dsl.module

val iosClientModule = module {
    single<UrlLauncher> { IOSUrlLauncher() }
    single<NotificationServiceController> {
        NotificationServiceController().apply {
            this.registerBackgroundTask()
        }
    }
}