package network.bisq.mobile.node.common.di

import network.bisq.mobile.node.main.NodeMainPresenter
import network.bisq.mobile.node.network.presentation.connections.NetworkConnectionsPresenter
import network.bisq.mobile.node.network.presentation.my_node.NetworkMyNodePresenter
import network.bisq.mobile.node.network.presentation.network.NetworkPresenter
import network.bisq.mobile.node.settings.backup.presentation.BackupPresenter
import network.bisq.mobile.node.settings.faqs.FaqNodePresenter
import network.bisq.mobile.node.settings.settings.NodeSettingsPresenter
import network.bisq.mobile.node.startup.onboarding.NodeOnboardingPresenter
import network.bisq.mobile.node.startup.splash.NodeSplashPresenter
import network.bisq.mobile.node.tabs.dashboard.NodeDashboardPresenter
import network.bisq.mobile.node.tabs.more.NodeMiscItemsPresenter
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManager
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManagerImpl
import network.bisq.mobile.presentation.common.share.AndroidShareFileService
import network.bisq.mobile.presentation.common.share.ShareFileService
import network.bisq.mobile.presentation.common.ui.animation.AnimationSettings
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarPresenter
import network.bisq.mobile.presentation.main.AppPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offerbook.OfferbookPresenter
import network.bisq.mobile.presentation.settings.faqs.FaqPresenter
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
        single<ShareFileService> { AndroidShareFileService(androidContext()) }

        // Node embeds the full bisq2 stack, so the low-RAM animation lock applies here
        single { AnimationSettings(get(), get(), applyDeviceLock = true) }

        factory<SettingsPresenter> { NodeSettingsPresenter(get(), get(), get(), get(), get(), get()) }

        factory {
            NodeOnboardingPresenter(
                get(),
                get(),
                get(),
            )
        } bind OnboardingPresenter::class

        factory<OfferbookPresenter> {
            OfferbookPresenter(
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

        factory<TopBarPresenter> { TopBarPresenter(get(), get(), get(), get(), get()) } bind ITopBarPresenter::class

        factory<MiscItemsPresenter> { NodeMiscItemsPresenter(get(), get()) }

        factory<FaqPresenter> { FaqNodePresenter(get()) }

        factory { NetworkPresenter(get(), get(), get()) }

        factory { NetworkConnectionsPresenter(get(), get()) }

        factory { NetworkMyNodePresenter(get(), get(), get()) }

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

        factory<NodeSplashPresenter> {
            NodeSplashPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        } bind SplashPresenter::class

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
