package network.bisq.mobile.presentation.di

import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val presentationModule = module {
    single<MainPresenter> { MainPresenter(get()) } bind AppPresenter::class
}