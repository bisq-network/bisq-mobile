package network.bisq.mobile.client.common.di

import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.presentation.common.di.presentationModule

val clientModules = listOf(domainModule, presentationModule, clientDomainModule, clientPresentationModule)
