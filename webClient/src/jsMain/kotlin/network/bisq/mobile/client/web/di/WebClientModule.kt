package network.bisq.mobile.client.web.di

import network.bisq.mobile.client.service.user_profile.ClientCatHashService
import network.bisq.mobile.client.web.WebClientMainPresenter
import network.bisq.mobile.domain.UrlLauncher
import network.bisq.mobile.domain.WebUrlLauncher
import network.bisq.mobile.domain.service.network.ClientConnectivityService
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import network.bisq.mobile.service.WebClientCatHashService
import org.koin.dsl.bind
import org.koin.dsl.module

val webClientModule = module {
    single<UrlLauncher> { WebUrlLauncher() }
    single { WebClientCatHashService() } bind ClientCatHashService::class
    single { ClientConnectivityService(get()) } bind ConnectivityService::class
    
    single<MainPresenter> {
        WebClientMainPresenter(
            get(), get(), get(), get(), get(), get(), get(), get(), get(), get()
        )
    } bind AppPresenter::class
}