package network.bisq.mobile.presentation.di

import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(listOf(presentationModule))
    }
}