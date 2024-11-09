package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.data.repository.GreetingRepository
import org.koin.dsl.module

val domainModule = module {
    single { GreetingRepository() }
}