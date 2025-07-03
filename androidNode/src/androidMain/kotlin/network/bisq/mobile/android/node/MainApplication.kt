package network.bisq.mobile.android.node

import android.app.Application
import android.content.Context
import android.os.Process
import bisq.common.facades.FacadeProvider
import bisq.common.facades.android.AndroidGuavaFacade
import network.bisq.mobile.android.node.service.facades.CustomAndroidJdkFacade
import network.bisq.mobile.android.node.di.androidNodeModule
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.di.serviceModule
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.di.presentationModule
import java.io.File
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.security.Security

class MainApplication : Application(), Logging {
    companion object {
        private val nodeModules = listOf(domainModule, serviceModule, presentationModule, androidNodeModule)

        fun setupKoinDI(appContext: Context) {
            // very important to avoid issues from the abuse of DI single {} singleton instances
            stopKoin()
            startKoin {
                androidContext(appContext)
                // order is important, last one is picked for each interface/class key
                modules(nodeModules)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        setupKoinDI(this)
        setupBisqCoreStatics()
        // Note: Tor initialization is now handled in NodeApplicationBootstrapFacade
        // as the very first step of the bootstrap process
//        setupTorSystemProperties()
        log.i { "Bisq Node Application Created" }
    }

    private fun setupBisqCoreStatics() {
        val isEmulator = isEmulator()
//        TODO this is part of Bisq v2.1.8, uncomment when developing against latest snapshot
//        val clearNetFacade = if (isEmulator) {
//            AndroidEmulatorAddressTypeFacade()
//        } else {
//            LANAddressTypeFacade()
//        }
//        FacadeProvider.setClearNetAddressTypeFacade(clearNetFacade)
        FacadeProvider.setJdkFacade(CustomAndroidJdkFacade(Process.myPid()))
        FacadeProvider.setGuavaFacade(AndroidGuavaFacade())

        // Androids default BC version does not support all algorithms we need, thus we remove
        // it and add our BC provider
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
        log.d { "Configured bisq2 for Android${if (isEmulator) " emulator" else ""}" }
    }

    /**
     * Set up system properties to tell Bisq2 to use external Tor
     * This must be done BEFORE AndroidApplicationService is created
     * and ensures Bisq2 has consistent behaviour waiting for external tor
     * @deprecated this is hanbdled by the TorBootstrapOrchestrator,
     * we can delete this when we are sure bootstrap is consistently reliable
     */
    private fun setupTorSystemProperties() {
        println("🔧🔧🔧 Setting up system properties to force Bisq2 to skip Tor launch")
        android.util.Log.i("MainApplication", "🔧🔧🔧 Setting up system properties to force Bisq2 to skip Tor launch")
        log.i { "🔧 Setting up system properties to force Bisq2 to skip Tor launch" }

        // Backup approach: Set system properties that might influence Tor behavior
        System.setProperty("bisq.network.transport", "TOR")

        // Set SOCKS proxy properties to redirect traffic to our kmp-tor
        System.setProperty("socksProxyHost", "127.0.0.1")
        System.setProperty("socksProxyPort", "9050") // Will be updated when kmp-tor is ready
        System.setProperty("socksProxyVersion", "5")

        // Set Bisq-specific SOCKS properties
        System.setProperty("bisq.torSocksHost", "127.0.0.1")
        System.setProperty("bisq.torSocksPort", "9050") // Will be updated when kmp-tor is ready

        log.i { "🔄 Configured system properties for SOCKS proxy hijacking" }
        log.i { "   Strategy: Force Bisq2 to skip embedded Tor and use our kmp-tor via SOCKS" }
    }

    private fun isEmulator(): Boolean {
        return android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(android.os.Build.PRODUCT);
    }
}
