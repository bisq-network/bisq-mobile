package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.common.presentation.support.ClientSupportPresenter
import network.bisq.mobile.client.common.presentation.top_bar.ClientTopBarPresenter
import network.bisq.mobile.client.main.ClientMainPresenter
import network.bisq.mobile.client.offerbook.ClientOfferbookPresenter
import network.bisq.mobile.client.settings.faqs.FaqClientPresenter
import network.bisq.mobile.client.splash.ClientSplashPresenter
import network.bisq.mobile.client.tabs.more.ClientMiscItemsPresenter
import network.bisq.mobile.client.trusted_node_setup.TrustedNodeSetupPresenter
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.main.AppPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offerbook.OfferbookPresenter
import network.bisq.mobile.presentation.settings.faqs.FaqPresenter
import network.bisq.mobile.presentation.startup.splash.SplashPresenter
import network.bisq.mobile.presentation.tabs.more.MiscItemsPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val clientPresentationModule =
    module {
        // Connect is a lightweight client with no embedded node, so don't use device-lock.
        single { AnimationSettings(get(), get(), applyDeviceLock = false) }

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

        factory {
            ClientSplashPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        } bind SplashPresenter::class

        factory<OfferbookPresenter> {
            ClientOfferbookPresenter(
                get(),
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
        }

        factory<TrustedNodeSetupPresenter> {
            TrustedNodeSetupPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }

        factory<TopBarPresenter> {
            ClientTopBarPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        } bind ITopBarPresenter::class

        factory<MiscItemsPresenter> { ClientMiscItemsPresenter(get(), get()) }

        factory<FaqPresenter> { FaqClientPresenter(get()) }

        factory<ClientSupportPresenter> {
            ClientSupportPresenter(
                get(),
                get(),
            )
        }
    }
