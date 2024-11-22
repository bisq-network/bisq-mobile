package network.bisq.mobile.domain.di

import org.koin.core.qualifier.named
import org.koin.dsl.module

val iosModule = module {
    single<String>(named("ApiBaseUrl")) { provideApiBaseUrl() }
}

fun provideApiBaseUrl(): String {
    return "localhost"
}