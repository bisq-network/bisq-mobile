package network.bisq.mobile.client.common.di

import network.bisq.mobile.client.common.domain.service.user_profile.ClientCatHashService
import network.bisq.mobile.client.common.domain.utils.IosClientCatHashService
import network.bisq.mobile.client.onboarding.ClientOnboardingPresenter
import network.bisq.mobile.domain.getStorageDir
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.IosDeviceInfoProvider
import network.bisq.mobile.presentation.PlatformSettingsManager
import network.bisq.mobile.presentation.PlatformSettingsManagerImpl
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val iosClientPresentationModule = module {
    single { IosClientCatHashService(getStorageDir()) } bind ClientCatHashService::class

    single<IOnboardingPresenter> {
        ClientOnboardingPresenter(
            get(),
            get(),
            get()
        )
    } bind IOnboardingPresenter::class

    single<DeviceInfoProvider> { IosDeviceInfoProvider() }

    single<PlatformSettingsManager> {
        PlatformSettingsManagerImpl()
    }
}