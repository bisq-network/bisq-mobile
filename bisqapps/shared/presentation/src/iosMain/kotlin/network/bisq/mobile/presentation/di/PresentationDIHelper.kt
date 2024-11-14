package network.bisq.mobile.presentation.di

import network.bisq.mobile.domain.di.domainModule
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(listOf(domainModule, presentationModule))
    }
}