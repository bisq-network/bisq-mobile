package network.bisq.mobile.client.di

import network.bisq.mobile.android.node.main.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.presentation.AndroidClientMainPresenter
import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.domain.client.main.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.domain.client.main.user_profile.UserProfileApiGateway
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val androidClientModule = module {
    single<ApplicationBootstrapFacade> { ClientApplicationBootstrapFacade() }

    single { ApiRequestService(get(), "10.0.2.2") }
    single { UserProfileApiGateway(get()) }
    single<UserProfileServiceFacade> { ClientUserProfileServiceFacade(get()) }
    single<MainPresenter> { AndroidClientMainPresenter(get()) } bind AppPresenter::class
}
