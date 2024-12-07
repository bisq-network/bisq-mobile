package network.bisq.mobile.domain.di

import org.koin.core.qualifier.named
import org.koin.dsl.module

val iosClientModule = module {
    single(named("RestApiHost")) { provideRestApiHost() }
    single(named("WebsocketApiHost")) { provideWebsocketHost() }
}

fun provideRestApiHost(): String {
    return "localhost"
}
fun provideWebsocketHost(): String {
    return "localhost"
}