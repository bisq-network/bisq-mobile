package network.bisq.mobile.presentation.di

import network.bisq.mobile.client.service.user_profile.ClientCatHashService
import network.bisq.mobile.domain.getStorageDir
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.IosDeviceInfoProvider
import network.bisq.mobile.presentation.ui.presentation.ClientOnboardingPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.service.IosClientCatHashService
import org.koin.dsl.bind
import org.koin.dsl.module

val iosPresentationModule = module {
    single { IosClientCatHashService(getStorageDir()) } bind ClientCatHashService::class

    single<IOnboardingPresenter> {
        ClientOnboardingPresenter(
            get(),
            get(),
            get()
        )
    } bind IOnboardingPresenter::class

    single<DeviceInfoProvider> { IosDeviceInfoProvider() }
}