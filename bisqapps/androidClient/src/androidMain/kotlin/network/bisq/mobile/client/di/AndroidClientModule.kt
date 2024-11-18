package network.bisq.mobile.client.di

import network.bisq.mobile.client.service.ApiRequestService
import network.bisq.mobile.domain.client.main.user_profile.ClientUserProfileModel
import network.bisq.mobile.domain.client.main.user_profile.ClientUserProfileServiceFacade
import network.bisq.mobile.domain.client.main.user_profile.UserProfileApiGateway
import network.bisq.mobile.domain.data.repository.UserProfileRepository
import network.bisq.mobile.domain.user_profile.UserProfileModel
import network.bisq.mobile.domain.user_profile.UserProfileServiceFacade
import org.koin.dsl.module

val androidClientModule = module {
    single<UserProfileModel> { ClientUserProfileModel() }
    single { ApiRequestService("10.0.2.2") }
    single { UserProfileApiGateway(get()) }
    single<UserProfileServiceFacade> { ClientUserProfileServiceFacade(get(), get()) }
    single { UserProfileRepository(get(), get()) }

}
