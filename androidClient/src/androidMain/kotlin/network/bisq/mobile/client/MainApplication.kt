package network.bisq.mobile.client

import android.app.Application
import android.content.Context
import network.bisq.mobile.client.di.androidClientModule
import network.bisq.mobile.client.di.clientModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.di.serviceModule
import network.bisq.mobile.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

class MainApplication: Application() {
    companion object {
        private val clientModules = listOf(domainModule, serviceModule, presentationModule, clientModule, androidClientModule)

        fun setupKoinDI(appContext: Context) {
            // very important to avoid issues from the abuse of DI single {} singleton instances
            stopKoin()
            startKoin {
                androidContext(appContext)
                // order is important, last one is picked for each interface/class key
                modules(clientModules)
            }
        }
    }
    override fun onCreate() {
        super.onCreate()

        setupKoinDI(this)
    }
}
