package network.bisq.mobile.client.di

import network.bisq.mobile.client.AndroidClientMainPresenter
import network.bisq.mobile.client.service.user_profile.ClientCatHashService
import network.bisq.mobile.domain.AndroidUrlLauncher
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.service.AppForegroundController
import network.bisq.mobile.domain.service.ForegroundDetector
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
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

    single { ClientConnectivityService(get()) } bind ConnectivityService::class

    single<AppForegroundController> { AppForegroundController(androidContext()) } bind ForegroundDetector::class
    single<NotificationServiceController> {
        NotificationServiceController(get())
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
}