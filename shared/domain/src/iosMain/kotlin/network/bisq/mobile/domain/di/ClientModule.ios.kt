package network.bisq.mobile.domain.di

import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import org.koin.core.qualifier.named
import org.koin.dsl.module

val iosClientModule = module {
    single<NotificationServiceController> {
        NotificationServiceController().apply {
            this.registerBackgroundTask()
        }
    }
}