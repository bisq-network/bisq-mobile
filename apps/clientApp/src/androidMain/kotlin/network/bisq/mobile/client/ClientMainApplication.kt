package network.bisq.mobile.client

import network.bisq.mobile.client.di.androidClientModule
import network.bisq.mobile.client.di.clientModule
import network.bisq.mobile.client.di.serviceModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.presentation.MainApplication
import network.bisq.mobile.presentation.di.presentationModule
import org.koin.android.ext.android.get
import org.koin.core.module.Module

/**
 * Android Bisq Connect Application definition
 */
class ClientMainApplication : MainApplication() {
    override fun getKoinModules(): List<Module> {
        return listOf(domainModule, serviceModule, presentationModule, clientModule, androidClientModule)
    }

    override fun onCreated() {
        // We start here the initialisation (non blocking) of tor and the service facades.
        // The lifecycle of those is tied to the lifecycle of the Application/Process not to the lifecycle of the MainActivity.
        val applicationLifecycleService: ClientApplicationLifecycleService = get()
        applicationLifecycleService.initialize()
        log.i { "Bisq Client Application Created" }
    }
}
