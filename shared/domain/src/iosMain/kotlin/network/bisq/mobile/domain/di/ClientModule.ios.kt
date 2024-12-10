package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.service.controller.NotificationServiceController
import org.koin.core.qualifier.named
import org.koin.dsl.module

val iosClientModule = module {
    single(named("RestApiHost")) { provideRestApiHost() }
    single(named("WebsocketApiHost")) { provideWebsocketHost() }

    single<NotificationServiceController> {
        NotificationServiceController().apply {
            this.registerBackgroundTask()
        }
    }
}

fun provideRestApiHost(): String {
    return "localhost"
}
fun provideWebsocketHost(): String {
    return "localhost"
}