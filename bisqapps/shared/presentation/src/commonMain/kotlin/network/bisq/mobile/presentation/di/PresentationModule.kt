package network.bisq.mobile.presentation.di

import network.bisq.mobile.presentation.MainPresenter
import org.koin.dsl.module

val presentationModule = module {
    single { MainPresenter(get()) }
}