package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.common.presentation.top_bar.ClientTopBarPresenter
import network.bisq.mobile.client.main.ClientMainPresenter
import network.bisq.mobile.client.offerbook.ClientOfferbookPresenter
import network.bisq.mobile.client.settings.presentation.ClientMiscItemsPresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookPresenter
import network.bisq.mobile.client.splash.ClientSplashPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import network.bisq.mobile.client.trusted_node_setup.TrustedNodeSetupPresenter
import network.bisq.mobile.presentation.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val clientPresentationModule = module {
    single<MainPresenter> {
        ClientMainPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
        )
    } bind AppPresenter::class

    single<SplashPresenter> {
        ClientSplashPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    single<OfferbookPresenter> {
        ClientOfferbookPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    single<TrustedNodeSetupPresenter> {
        TrustedNodeSetupPresenter(
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    single<TopBarPresenter> {
        ClientTopBarPresenter(
            get(),
            get(),
            get(),
            get()
        )
    } bind ITopBarPresenter::class


    factory<MiscItemsPresenter> { ClientMiscItemsPresenter(get(), get()) }

}