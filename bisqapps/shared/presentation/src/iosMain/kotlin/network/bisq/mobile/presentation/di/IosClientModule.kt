package network.bisq.mobile.presentation.di

import network.bisq.mobile.android.node.main.bootstrap.ClientApplicationBootstrapFacade
import network.bisq.mobile.android.node.main.bootstrap.ClientApplicationBootstrapModel
import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.domain.client.main.user_profile.ClientUserProfileModel
import network.bisq.mobile.domain.client.main.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.domain.client.main.user_profile.UserProfileApiGateway
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.data.repository.main.bootstrap.ApplicationBootstrapModel
import network.bisq.mobile.domain.user_profile.UserProfileModel
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade
import org.koin.dsl.module

val iosClientModule = module {
    single<ApplicationBootstrapModel> { ClientApplicationBootstrapModel() }
    single<ApplicationBootstrapFacade> { ClientApplicationBootstrapFacade(get()) }

    single<UserProfileModel> { ClientUserProfileModel() }
    single { ApiRequestService(get(), "localhost") }
    single { UserProfileApiGateway(get()) }
    single<UserProfileServiceFacade> { ClientUserProfileServiceFacade(get(), get()) }

}
