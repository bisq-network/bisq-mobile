package network.bisq.mobile.android.node

import android.app.Application
import android.os.Process
import bisq.common.facades.FacadeProvider
import bisq.common.facades.android.AndroidGuavaFacade
import bisq.common.facades.android.AndroidJdkFacade
import bisq.common.network.AndroidEmulatorLocalhostFacade
import network.bisq.mobile.android.node.di.androidNodeModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.presentation.di.presentationModule
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import java.security.Security

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        setupKoinDI()
        setupBisqCoreStatics()
    }

    private fun setupBisqCoreStatics() {
        FacadeProvider.setLocalhostFacade(AndroidEmulatorLocalhostFacade())
        FacadeProvider.setJdkFacade(AndroidJdkFacade(Process.myPid()))
        FacadeProvider.setGuavaFacade(AndroidGuavaFacade())

        // Androids default BC version does not support all algorithms we need, thus we remove
        // it and add our BC provider
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
    }

    private fun setupKoinDI() {
        // Initialize Koin only if it hasn't been initialized - fix issue running emulated robo instrumentation unit tests
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@MainApplication)
                // order is important, last one is picked for each interface/class key
                modules(listOf(domainModule, presentationModule, androidNodeModule))
            }
        }
    }
}
