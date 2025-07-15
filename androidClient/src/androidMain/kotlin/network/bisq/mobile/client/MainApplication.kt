package network.bisq.mobile.client

import android.content.Context
import network.bisq.mobile.client.di.androidClientModule
import network.bisq.mobile.client.di.clientModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.di.serviceModule
import network.bisq.mobile.presentation.BisqMainApplication
import network.bisq.mobile.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Android Bisq Connect Application definition
 */
class MainApplication : BisqMainApplication() {
    
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
    
    override fun setupKoinDI() {
        setupKoinDI(this)
    }
}
