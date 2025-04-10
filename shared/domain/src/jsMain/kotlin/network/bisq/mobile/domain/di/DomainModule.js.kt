package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * JS-specific service module for dependency injection
 */
val serviceModule = module {
    // Provide JS-specific implementations

    single<AppForegroundController> { AppForegroundController() } bind ForegroundDetector::class
    single<NotificationServiceController> { NotificationServiceController(get()) }
    
    single<OpenTradesNotificationService> {
        OpenTradesNotificationService(get(), get())
    }
}