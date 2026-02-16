package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.common.presentation.support.ClientSupportPresenter
import network.bisq.mobile.client.common.presentation.top_bar.ClientTopBarPresenter
import network.bisq.mobile.client.main.ClientMainPresenter
import network.bisq.mobile.client.offerbook.ClientOfferbookPresenter
import network.bisq.mobile.client.splash.ClientSplashPresenter
import network.bisq.mobile.client.tabs.more.ClientMiscItemsPresenter
import network.bisq.mobile.client.trusted_node_setup.TrustedNodeSetupPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.main.AppPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offerbook.OfferbookPresenter
import network.bisq.mobile.presentation.startup.splash.SplashPresenter
import network.bisq.mobile.presentation.tabs.more.MiscItemsPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val clientPresentationModule =
    module {
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
                get(),
            )
        } bind AppPresenter::class

        factory<SplashPresenter> {
            ClientSplashPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
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
                get(),
            )
        }

        factory<TrustedNodeSetupPresenter> {
            TrustedNodeSetupPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }

        single<TopBarPresenter> {
            ClientTopBarPresenter(
                get(),
                get(),
                get(),
                get(),
            )
        } bind ITopBarPresenter::class

        factory<MiscItemsPresenter> { ClientMiscItemsPresenter(get(), get()) }

        single<ClientSupportPresenter> {
            ClientSupportPresenter(
                get(),
                get(),
            )
        }
    }
