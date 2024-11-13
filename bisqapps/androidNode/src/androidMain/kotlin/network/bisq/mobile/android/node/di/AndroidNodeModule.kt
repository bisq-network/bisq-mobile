package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.AndroidNodeGreetingFactory
import network.bisq.mobile.android.node.presentation.MainNodePresenter
import network.bisq.mobile.domain.GreetingFactory
import network.bisq.mobile.presentation.MainPresenter
import org.koin.dsl.module

val androidNodeModule = module {
    single<GreetingFactory> { AndroidNodeGreetingFactory() }
    single<MainPresenter> { MainNodePresenter(get()) }
}