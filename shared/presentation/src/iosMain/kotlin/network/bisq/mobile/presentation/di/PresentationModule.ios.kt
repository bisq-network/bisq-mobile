package network.bisq.mobile.presentation.di

import network.bisq.mobile.client.user_profile.ClientCatHashService
import network.bisq.mobile.service.IosClientCatHashService
import org.koin.dsl.bind
import org.koin.dsl.module

val iosPresentationModule = module {
    single { IosClientCatHashService() } bind ClientCatHashService::class
}