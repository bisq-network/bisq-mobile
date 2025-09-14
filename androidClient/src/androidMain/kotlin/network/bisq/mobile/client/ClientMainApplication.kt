package network.bisq.mobile.client

import network.bisq.mobile.client.di.androidClientModule
import network.bisq.mobile.client.di.clientModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.di.serviceModule
import network.bisq.mobile.presentation.MainApplication
import network.bisq.mobile.presentation.di.presentationModule
import org.koin.core.module.Module

/**
 * Android Bisq Connect Application definition
 */
class ClientMainApplication : MainApplication() {
    override fun getKoinModules(): List<Module> {
        return listOf(domainModule, serviceModule, presentationModule, clientModule, androidClientModule)
    }
}
