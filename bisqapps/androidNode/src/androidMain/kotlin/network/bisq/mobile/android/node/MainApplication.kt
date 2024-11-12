package network.bisq.mobile.android.node

import android.app.Application
import network.bisq.mobile.android.node.di.androidNodeModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.presentation.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            modules(listOf(androidNodeModule, domainModule, presentationModule)) 
        }
    }
}
