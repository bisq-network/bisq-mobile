package network.bisq.mobile.android.node.di

import network.bisq.mobile.android.node.AndroidNodeGreeting
import network.bisq.mobile.android.node.domain.data.repository.NodeGreetingRepository
import network.bisq.mobile.android.node.presentation.MainNodePresenter
import network.bisq.mobile.domain.data.repository.SingleObjectRepository
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodeModule = module {
    // this one is for example properties, will be eliminated soon
    single<NodeGreetingRepository> { NodeGreetingRepository() }
    // this line showcases both, the posibility to change behaviour of the app by changing one definiton
    // and binding the same obj to 2 different abstractions
    single<MainPresenter> { MainNodePresenter(get()) } bind AppPresenter::class
}