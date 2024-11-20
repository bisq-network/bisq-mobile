package network.bisq.mobile.presentation.di

import network.bisq.mobile.android.node.main.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.domain.client.main.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.domain.client.main.user_profile.UserProfileApiGateway
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade
import network.bisq.mobile.presentation.IosClientMainPresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.AppPresenter
import org.koin.dsl.bind
import org.koin.dsl.module

val iosClientModule = module {
    single<ApplicationBootstrapFacade> { ClientApplicationBootstrapFacade() }

    single { ApiRequestService(get(), "localhost") }
    single { UserProfileApiGateway(get()) }
    single<UserProfileServiceFacade> { ClientUserProfileServiceFacade(get()) }

    single<MainPresenter> { IosClientMainPresenter(get()) } bind AppPresenter::class
}
