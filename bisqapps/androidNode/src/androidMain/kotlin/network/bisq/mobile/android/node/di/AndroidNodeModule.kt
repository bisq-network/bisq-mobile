package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.AndroidNodeGreetingFactory
import network.bisq.mobile.domain.GreetingFactory
import org.koin.dsl.module

val androidNodeModule = module {
    single<GreetingFactory> { AndroidNodeGreetingFactory() }
}