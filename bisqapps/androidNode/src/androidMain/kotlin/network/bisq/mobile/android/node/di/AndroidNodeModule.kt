package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.AndroidNodeGreeting
import network.bisq.mobile.android.node.AndroidNodeGreetingFactory
import network.bisq.mobile.android.node.presentation.MainNodePresenter
import network.bisq.mobile.domain.data.model.GreetingFactory
import network.bisq.mobile.domain.data.repository.SingleObjectRepository
import network.bisq.mobile.presentation.MainPresenter
import org.koin.dsl.module

val androidNodeModule = module {
    // this one is for example properties, will be eliminated soon
    single<SingleObjectRepository<AndroidNodeGreeting>> { SingleObjectRepository() }
    single<MainPresenter> { MainNodePresenter(get()) }
}