package network.bisq.mobile.client

import android.app.Application
import network.bisq.mobile.client.di.androidModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext

import org.koin.core.context.startKoin

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            modules(domainModule + presentationModule + androidModule)
        }
    }
}