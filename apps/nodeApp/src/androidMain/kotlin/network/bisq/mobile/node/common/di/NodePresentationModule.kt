package network.bisq.mobile.node.common.di

import network.bisq.mobile.node.main.NodeMainPresenter
import network.bisq.mobile.node.settings.backup.presentation.BackupPresenter
import network.bisq.mobile.node.settings.settings.NodeSettingsPresenter
import network.bisq.mobile.node.startup.onboarding.NodeOnboardingPresenter
import network.bisq.mobile.node.startup.splash.NodeSplashPresenter
import network.bisq.mobile.node.tabs.dashboard.NodeDashboardPresenter
import network.bisq.mobile.node.tabs.more.NodeMiscItemsPresenter
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManager
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManagerImpl
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.main.AppPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offerbook.OfferbookPresenter
import network.bisq.mobile.presentation.settings.settings.IGeneralSettingsPresenter
import network.bisq.mobile.presentation.settings.settings.SettingsPresenter
import network.bisq.mobile.presentation.startup.onboarding.OnboardingPresenter
import network.bisq.mobile.presentation.startup.splash.SplashPresenter
import network.bisq.mobile.presentation.tabs.dashboard.DashboardPresenter
import network.bisq.mobile.presentation.tabs.more.MiscItemsPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidNodePresentationModule =
    module {
        factory<SettingsPresenter> { NodeSettingsPresenter(get(), get(), get()) } bind IGeneralSettingsPresenter::class

        factory {
            NodeOnboardingPresenter(
                get(),
                get(),
                get(),
            )
        } bind OnboardingPresenter::class

        single<OfferbookPresenter> {
            OfferbookPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }

        single<TopBarPresenter> { TopBarPresenter(get(), get(), get(), get()) } bind ITopBarPresenter::class

        factory<MiscItemsPresenter> { NodeMiscItemsPresenter(get(), get()) }

        factory<BackupPresenter> { BackupPresenter(get(), get()) }

        single<MainPresenter> {
            NodeMainPresenter(
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
            NodeSplashPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
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
                get(), // pushNotificationServiceFacade
            )
        }

        single<PlatformSettingsManager> {
            PlatformSettingsManagerImpl(androidContext())
        }
    }
