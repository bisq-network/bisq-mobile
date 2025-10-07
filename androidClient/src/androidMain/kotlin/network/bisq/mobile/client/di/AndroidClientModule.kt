package network.bisq.mobile.client.di

import network.bisq.mobile.client.AndroidClientMainPresenter
import network.bisq.mobile.client.ClientApplicationLifecycleService
import network.bisq.mobile.client.presentation.ClientOnboardingPresenter
import network.bisq.mobile.client.service.user_profile.ClientCatHashService
import network.bisq.mobile.client.utils.ClientVersionProvider
import network.bisq.mobile.domain.AndroidUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.utils.AndroidDeviceInfoProvider
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.presentation.ui.uicases.startup.IOnboardingPresenter
import network.bisq.mobile.service.AndroidClientCatHashService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module


val androidClientModule = module {
    single<UrlLauncher> { AndroidUrlLauncher(androidContext()) }
    single {
        val context = androidContext()
        val filesDir = context.filesDir.absolutePath
        AndroidClientCatHashService(context, filesDir)
    } bind ClientCatHashService::class

    single<IOnboardingPresenter> {
        ClientOnboardingPresenter(
            get(),
            get(),
            get()
        )
    }

    single<DeviceInfoProvider> { AndroidDeviceInfoProvider(androidContext()) }

    single<VersionProvider> { ClientVersionProvider() }

    single<ClientApplicationLifecycleService> {
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
            get(),
            get(),
            get()
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
        )
    } bind AppPresenter::class
}
