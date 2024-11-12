package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.DefaultGreetingFactory
import network.bisq.mobile.domain.GreetingFactory
import network.bisq.mobile.domain.data.repository.GreetingRepository
import org.koin.dsl.module

val domainModule = module {
    single<GreetingFactory> { DefaultGreetingFactory() }
    single { GreetingRepository(get()) }
}
