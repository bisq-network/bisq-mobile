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

        // VERY AGGRESSIVE LOGGING TO ENSURE WE SEE THIS
        println("🚀🚀🚀 MainApplication.onCreate() - STARTING WITH NEW TOR FIX")
        android.util.Log.i("MainApplication", "🚀🚀🚀 MainApplication.onCreate() - STARTING WITH NEW TOR FIX")
        log.i { "🚀🚀🚀 MainApplication.onCreate() - STARTING WITH NEW TOR FIX" }

        setupKoinDI(this)
        setupBisqCoreStatics()
        setupTorSystemProperties()
        // Note: Tor initialization is now handled in NodeApplicationBootstrapFacade
        // as the very first step of the bootstrap process

        println("✅✅✅ MainApplication.onCreate() completed successfully")
        android.util.Log.i("MainApplication", "✅✅✅ MainApplication.onCreate() completed successfully")
        log.i { "✅✅✅ MainApplication.onCreate() completed successfully" }
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
        println("🔧🔧🔧 Setting up system properties to force Bisq2 to skip Tor launch")
        android.util.Log.i("MainApplication", "🔧🔧🔧 Setting up system properties to force Bisq2 to skip Tor launch")
        log.i { "🔧 Setting up system properties to force Bisq2 to skip Tor launch" }

        // CRITICAL: Try to set the TOR_SKIP_LAUNCH environment variable that Bisq2 checks
        // This is the most reliable way to prevent Bisq2 from starting its own Tor
        try {
            setEnvironmentVariable("TOR_SKIP_LAUNCH", "1")
            log.i { "✅ Set TOR_SKIP_LAUNCH environment variable to 1" }
        } catch (e: Exception) {
            log.w(e) { "⚠️ Could not set TOR_SKIP_LAUNCH environment variable, trying system properties" }
        }

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

    /**
     * Attempt to set an environment variable using reflection
     * This is a hack but necessary to set TOR_SKIP_LAUNCH for Bisq2
     */
    private fun setEnvironmentVariable(name: String, value: String) {
        try {
            val processEnvironment = Class.forName("java.lang.ProcessEnvironment")
            val theEnvironmentField = processEnvironment.getDeclaredField("theEnvironment")
            theEnvironmentField.isAccessible = true
            val env = theEnvironmentField.get(null) as MutableMap<String, String>
            env[name] = value

            val theCaseInsensitiveEnvironmentField = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment")
            theCaseInsensitiveEnvironmentField.isAccessible = true
            val cienv = theCaseInsensitiveEnvironmentField.get(null) as MutableMap<String, String>
            cienv[name] = value

            log.i { "✅ Successfully set environment variable $name=$value" }
        } catch (e: Exception) {
            log.w(e) { "❌ Failed to set environment variable $name=$value" }
            throw e
        }
        System.setProperty("tor.control.port", "0") // Another alternative
        System.setProperty("tor.control.enabled", "false") // Explicitly disable control

        // Note: We're not generating external_tor.config since we're using SOCKS hijacking

        log.i { "✅ System properties set for external Tor usage" }
        log.i { "   Note: SOCKS port will be updated when our Tor daemon becomes ready" }

        // Log all the properties we set for debugging
        log.i { "🔍 System properties set:" }
        log.i { "   bisq.useExternalTor = ${System.getProperty("bisq.useExternalTor")}" }
        log.i { "   bisq.network.useExternalTor = ${System.getProperty("bisq.network.useExternalTor")}" }
        log.i { "   tor.external = ${System.getProperty("tor.external")}" }
        log.i { "   TOR_SKIP_LAUNCH = ${System.getProperty("TOR_SKIP_LAUNCH")}" }
        log.i { "   bisq.tor.skipLaunch = ${System.getProperty("bisq.tor.skipLaunch")}" }
        log.i { "   bisq.torSocksHost = ${System.getProperty("bisq.torSocksHost")}" }
        log.i { "   bisq.torSocksPort = ${System.getProperty("bisq.torSocksPort")}" }
        log.i { "   bisq.network.transport = ${System.getProperty("bisq.network.transport")}" }
    }

    /**
     * Generate initial external_tor.config file that tells Bisq2 to use external Tor
     * This must be called BEFORE AndroidApplicationService is created
     */
    private fun generateInitialExternalTorConfig() {
        try {
            log.i { "🔧 Generating initial external_tor.config for Bisq2" }

            val configContent = buildString {
                appendLine("# External Tor configuration for Bisq2")
                appendLine("# Generated at app startup to ensure Bisq2 uses external Tor")
                appendLine("# CRITICAL: This tells Bisq2 NOT to start its own Tor process")
                appendLine()
                appendLine("UseExternalTor 1")
                appendLine()
                appendLine("# Placeholder control port (will be updated when kmp-tor is ready)")
                appendLine("ControlPort 127.0.0.1:9051")
                appendLine("CookieAuthentication 0")
                appendLine("HashedControlPassword \"\"")
                appendLine()
                appendLine("# SOCKS proxy configuration (will be updated when Tor is ready)")
                appendLine("SocksPort 127.0.0.1:9050")
            }

            // Write to MULTIPLE locations where Bisq2 might look for external_tor.config

            // Location 1: /data/data/app/files/resources/external_tor.config
            val resourcesDir = File(filesDir, "resources")
            if (!resourcesDir.exists()) {
                resourcesDir.mkdirs()
            }
            val configFile1 = File(resourcesDir, "external_tor.config")
            configFile1.writeText(configContent)
            log.i { "✅ Generated initial external_tor.config at ${configFile1.absolutePath}" }

            // Location 2: /data/data/app/files/external_tor.config (root of files dir)
            val configFile2 = File(filesDir, "external_tor.config")
            configFile2.writeText(configContent)
            log.i { "✅ Generated initial external_tor.config at ${configFile2.absolutePath}" }

            // Location 3: /data/data/app/files/Bisq2_mobile/external_tor.config (Bisq2 app dir)
            val bisqDir = File(filesDir, "Bisq2_mobile")
            if (!bisqDir.exists()) {
                bisqDir.mkdirs()
            }
            val configFile3 = File(bisqDir, "external_tor.config")
            configFile3.writeText(configContent)
            log.i { "✅ Generated initial external_tor.config at ${configFile3.absolutePath}" }

            log.d { "Config content:\n$configContent" }

        } catch (e: Exception) {
            log.e(e) { "❌ Failed to generate initial external_tor.config" }
        }
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
