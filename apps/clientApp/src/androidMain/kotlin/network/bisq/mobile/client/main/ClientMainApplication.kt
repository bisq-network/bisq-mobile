package network.bisq.mobile.client.main

import network.bisq.mobile.client.common.di.androidClientDomainModule
import network.bisq.mobile.client.common.di.androidClientPresentationModule
import network.bisq.mobile.client.common.di.clientModules
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.presentation.main.MainApplication
import org.koin.android.ext.android.get
import org.koin.core.module.Module

/**
 * Android Bisq Connect Application definition
 */
class ClientMainApplication : MainApplication() {
    override fun getKoinModules(): List<Module> =
        clientModules +
            listOf(
                androidClientDomainModule,
                androidClientPresentationModule,
            )

    override fun onCreated() {
        // We start here the initialisation (non blocking) of tor and the service facades.
        // The lifecycle of those is tied to the lifecycle of the Application/Process not to the lifecycle of the MainActivity.
        val applicationLifecycleService: ApplicationLifecycleService = get()
        applicationLifecycleService.initialize()
        log.i { "Bisq Client Application Created" }
    }
}
