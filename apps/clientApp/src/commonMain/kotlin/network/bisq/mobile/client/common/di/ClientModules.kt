package network.bisq.mobile.client.common.di

import network.bisq.mobile.data.di.dataModule
import network.bisq.mobile.presentation.common.di.presentationModule

val clientModules = listOf(dataModule, presentationModule, clientDomainModule, clientPresentationModule)
