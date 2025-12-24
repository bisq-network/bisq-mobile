package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.common.domain.service.ClientApplicationLifecycleService
import network.bisq.mobile.domain.IOSUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.domain.utils.ClientVersionProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.notification.ForegroundServiceController
import network.bisq.mobile.presentation.common.notification.ForegroundServiceControllerImpl
import network.bisq.mobile.presentation.common.notification.NotificationController
import network.bisq.mobile.presentation.common.notification.NotificationControllerImpl
import network.bisq.mobile.presentation.common.service.OpenTradesNotificationService
import org.koin.dsl.bind
import org.koin.dsl.module

val iosClientDomainModule =
    module {
        single { AppForegroundController() } bind ForegroundDetector::class
        single { NotificationControllerImpl(get()) } bind NotificationController::class
        single { ForegroundServiceControllerImpl(get()) } bind ForegroundServiceController::class
        single {
            OpenTradesNotificationService(get(), get(), get(), get(), get())
        }

        single<ApplicationLifecycleService> {
            ClientApplicationLifecycleService(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single<UrlLauncher> { IOSUrlLauncher() }
        single<VersionProvider> { ClientVersionProvider() }
    }
