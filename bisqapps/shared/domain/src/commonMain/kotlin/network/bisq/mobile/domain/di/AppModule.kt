package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.data.repository.GreetingRepository
import org.koin.dsl.module

val appModule = module {
    single { GreetingRepository() }
}