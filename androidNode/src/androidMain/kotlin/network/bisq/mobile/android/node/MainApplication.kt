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
        setupTorSystemProperties()
        // Note: Tor initialization is now handled in NodeApplicationBootstrapFacade
        // as the very first step of the bootstrap process
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
     */
    private fun setupTorSystemProperties() {
        log.i { "🔧 Setting up system properties for external Tor usage" }

        // Set the primary flag that tells Bisq2 to use external Tor
        System.setProperty("bisq.useExternalTor", "true")
        System.setProperty("bisq.network.useExternalTor", "true")
        System.setProperty("tor.external", "true")

        // Set default SOCKS proxy properties (will be updated with actual port later)
        // Using default Tor SOCKS port as placeholder
        System.setProperty("bisq.torSocksHost", "127.0.0.1")
        System.setProperty("bisq.torSocksPort", "9050")
        System.setProperty("socksProxyHost", "127.0.0.1")
        System.setProperty("socksProxyPort", "9050")
        System.setProperty("socksProxyVersion", "5")

        // Network transport configuration
        System.setProperty("bisq.network.transport", "TOR")

        // Additional Tor configuration properties
        System.setProperty("bisq.torExternalControl", "true")
        System.setProperty("bisq.torControlHost", "127.0.0.1")
        System.setProperty("bisq.torControlPort", "0") // We don't expose control port

        log.i { "✅ System properties set for external Tor usage" }
        log.i { "   Note: SOCKS port will be updated when our Tor daemon becomes ready" }

        // Log all the properties we set for debugging
        log.i { "🔍 System properties set:" }
        log.i { "   bisq.useExternalTor = ${System.getProperty("bisq.useExternalTor")}" }
        log.i { "   bisq.network.useExternalTor = ${System.getProperty("bisq.network.useExternalTor")}" }
        log.i { "   tor.external = ${System.getProperty("tor.external")}" }
        log.i { "   bisq.torSocksHost = ${System.getProperty("bisq.torSocksHost")}" }
        log.i { "   bisq.torSocksPort = ${System.getProperty("bisq.torSocksPort")}" }
        log.i { "   bisq.network.transport = ${System.getProperty("bisq.network.transport")}" }
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
