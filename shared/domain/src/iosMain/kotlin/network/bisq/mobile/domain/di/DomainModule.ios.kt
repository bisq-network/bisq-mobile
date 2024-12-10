package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.service.controller.NotificationServiceController
import org.koin.core.qualifier.named
import org.koin.dsl.module

val iosDomainModule = module {
    single<String>(named("ApiBaseUrl")) { provideApiBaseUrl() }
    single<NotificationServiceController> { NotificationServiceController() }
}

fun provideApiBaseUrl(): String {
    return "localhost"
}