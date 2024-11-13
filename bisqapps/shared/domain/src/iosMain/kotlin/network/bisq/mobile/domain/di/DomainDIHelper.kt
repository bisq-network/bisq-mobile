package network.bisq.mobile.domain.di

import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(listOf(domainModule))
    }
}