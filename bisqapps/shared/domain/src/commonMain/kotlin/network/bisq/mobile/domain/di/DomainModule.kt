package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.data.model.DefaultGreetingFactory
import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.model.GreetingFactory
import network.bisq.mobile.domain.data.repository.SingleObjectRepository
import org.koin.dsl.module

val domainModule = module {
    single { SingleObjectRepository<Greeting>() }
}
