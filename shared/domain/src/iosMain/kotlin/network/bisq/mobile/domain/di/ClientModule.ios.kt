package network.bisq.mobile.domain.di

import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.domain.service.controller.NotificationServiceController
import org.koin.core.qualifier.named
import org.koin.dsl.module

val iosClientModule = module {
    single(named("ApiHost")) { provideApiHost() }
    single(named("WebsocketApiHost")) { provideWebsocketHost() }

    single<NotificationServiceController> {
        NotificationServiceController().apply {
            this.registerBackgroundTask()
        }
    }
}

fun provideApiHost(): String {
    return BuildConfig.WS_IOS_HOST.takeIf { it.isNotEmpty() } ?: "localhost"
}
fun provideWebsocketHost(): String {
    return BuildConfig.WS_IOS_HOST.takeIf { it.isNotEmpty() } ?: "localhost"
}