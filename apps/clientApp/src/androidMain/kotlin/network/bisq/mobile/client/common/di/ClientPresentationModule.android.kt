package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.common.domain.service.ClientApplicationLifecycleService
import network.bisq.mobile.client.common.domain.service.user_profile.ClientCatHashService
import network.bisq.mobile.client.common.domain.utils.AndroidClientCatHashService
import network.bisq.mobile.client.common.domain.utils.ClientVersionProvider
import network.bisq.mobile.client.main.AndroidClientMainPresenter
import network.bisq.mobile.client.onboarding.ClientOnboardingPresenter
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.utils.AndroidUrlLauncher
import network.bisq.mobile.data.utils.UrlLauncher
import network.bisq.mobile.domain.utils.AndroidDeviceInfoProvider
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManager
import network.bisq.mobile.presentation.common.platform_settings.PlatformSettingsManagerImpl
import network.bisq.mobile.presentation.common.share.AndroidShareFileService
import network.bisq.mobile.presentation.common.share.ShareFileService
import network.bisq.mobile.presentation.main.AppPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.startup.onboarding.OnboardingPresenter
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val androidClientPresentationModule =
    module {
        single<UrlLauncher> { AndroidUrlLauncher(androidContext()) }
        single {
            val context = androidContext()
            val filesDir = context.filesDir.absolutePath
            AndroidClientCatHashService(context, filesDir)
        } bind ClientCatHashService::class

        factory {
            ClientOnboardingPresenter(
                get(),
                get(),
                get(),
            )
        } bind OnboardingPresenter::class

        single<DeviceInfoProvider> { AndroidDeviceInfoProvider(androidContext()) }

        single<VersionProvider> { ClientVersionProvider() }

        single<ApplicationLifecycleService> {
            ClientApplicationLifecycleService(
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
                get(),
                get(),
                get(), // tradeRestrictingAlertServiceFacade
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(), // pushNotificationServiceFacade
                get(), // settingsRepository
                get(), // notificationController
            )
        }

        single<MainPresenter> {
            AndroidClientMainPresenter(
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

        single<PlatformSettingsManager> {
            PlatformSettingsManagerImpl(androidContext())
        }

        single<ShareFileService> { AndroidShareFileService(androidContext()) }
    }
