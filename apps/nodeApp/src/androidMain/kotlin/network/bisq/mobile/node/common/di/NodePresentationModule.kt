package network.bisq.mobile.node.common.di

import network.bisq.mobile.node.dashboard.NodeDashboardPresenter
import network.bisq.mobile.node.main.NodeMainPresenter
import network.bisq.mobile.node.onboarding.NodeOnboardingPresenter
import network.bisq.mobile.node.resources.NodeResourcesPresenter
import network.bisq.mobile.node.settings.NodeMiscItemsPresenter
import network.bisq.mobile.node.settings.NodeSettingsPresenter
import network.bisq.mobile.node.splash.NodeSplashPresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.PlatformSettingsManager
import network.bisq.mobile.presentation.PlatformSettingsManagerImpl
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.ui.uicases.DashboardPresenter
import network.bisq.mobile.presentation.ui.uicases.offerbook.OfferbookPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.IGeneralSettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.ResourcesPresenter
import network.bisq.mobile.presentation.ui.uicases.settings.SettingsPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.SplashPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodePresentationModule = module {
    factory<SettingsPresenter> { NodeSettingsPresenter(get(), get(), get()) } bind IGeneralSettingsPresenter::class

    factory<IOnboardingPresenter> {
        NodeOnboardingPresenter(
            get(),
            get(),
            get()
        )
    } bind IOnboardingPresenter::class

    single<OfferbookPresenter> {
        OfferbookPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    single<TopBarPresenter> { TopBarPresenter(get(), get(), get(), get()) } bind ITopBarPresenter::class

    factory<MiscItemsPresenter> { NodeMiscItemsPresenter(get(), get()) }

    factory<ResourcesPresenter> { NodeResourcesPresenter(get(), get(), get(), get()) }

    single<MainPresenter> {
        NodeMainPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind AppPresenter::class

    factory<SplashPresenter> {
        NodeSplashPresenter(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    }

    factory<DashboardPresenter> {
        NodeDashboardPresenter(
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

    single<PlatformSettingsManager> {
        PlatformSettingsManagerImpl(androidContext())
    }
}