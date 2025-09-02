package network.bisq.mobile.android.node

import android.annotation.SuppressLint
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.Process
import bisq.common.facades.FacadeProvider
import bisq.common.facades.android.AndroidGuavaFacade
import bisq.common.facades.android.AndroidJdkFacade
import bisq.common.network.clear_net_address_types.AndroidEmulatorAddressTypeFacade
import bisq.common.network.clear_net_address_types.LANAddressTypeFacade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import network.bisq.mobile.android.node.di.androidNodeModule
import network.bisq.mobile.android.node.service.offers.NodeOffersServiceFacade
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.di.serviceModule
import network.bisq.mobile.domain.service.offers.OffersServiceFacade
import network.bisq.mobile.presentation.BisqMainApplication
import network.bisq.mobile.presentation.di.presentationModule
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.security.Security
import org.koin.android.ext.android.inject

/**
 * Bisq Android Node Application definition
 * TODO consider uplift ComponentCallbacks2 to shared app to use also in connect apps
 */
class MainApplication : BisqMainApplication(), ComponentCallbacks2 {

    // Lazy inject to avoid circular dependencies during app startup
    private val nodeOffersServiceFacade: OffersServiceFacade? by inject()

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
    
    override fun setupKoinDI() {
        setupKoinDI(this)
    }

    override fun onCreated() {
        // Use runBlocking for essential system initialization that must complete before app continues
        // This is acceptable here because:
        // 1. It's Application.onCreate() - the right place for critical setup
        // 2. setupBisqCoreStatics() configures essential system components (BouncyCastle, Facades)
        // 3. The app cannot function without these being initialized
        // 4. It's a one-time operation during app startup
        runBlocking(Dispatchers.IO) {
            setupBisqCoreStatics()
        }
        // Note: MainApplication already implements ComponentCallbacks2, so onTrimMemory is automatically called
        // No need to registerComponentCallbacks(this) - that would cause infinite recursion
        // Note: Tor initialization is now handled in NodeApplicationBootstrapFacade
        log.i { "Bisq Node Application Created" }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        log.i { "System memory trim requested (level: $level)" }
        try {
            // Memory cleanup of data-intensive facades. More to be added if needed
            (nodeOffersServiceFacade as NodeOffersServiceFacade?).let {
                if (it == null) {
                    log.w { "Offers service not initialized, skipping memory trim" }
                } else {
                    log.i { "Trimming offers service memory" }
                    it.onTrimMemory(level)
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Error handling memory trim" }
        }
    }

    @SuppressLint("WrongConstant")
    @Deprecated("onLowMemory is deprecated in favor of onTrimMemory")
    override fun onLowMemory() {
        super.onLowMemory()
        log.w { "System low memory callback" }
        onTrimMemory(80) // TRIM_MEMORY_COMPLETE equivalent
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // do nth - Required by ComponentCallbacks2 interface
    }

    override fun isDebug(): Boolean {
        return BuildNodeConfig.IS_DEBUG
    }

    private suspend fun setupBisqCoreStatics() {
        withContext(Dispatchers.IO) {
            val isEmulator = isEmulator()
            val clearNetFacade = if (isEmulator) {
                AndroidEmulatorAddressTypeFacade()
            } else {
                LANAddressTypeFacade()
            }
            FacadeProvider.setClearNetAddressTypeFacade(clearNetFacade)
            FacadeProvider.setJdkFacade(AndroidJdkFacade(Process.myPid()))
            FacadeProvider.setGuavaFacade(AndroidGuavaFacade())

            // Androids default BC version does not support all algorithms we need, thus we remove
            // it and add our BC provider
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
            log.d { "Configured bisq2 for Android${if (isEmulator) " emulator" else ""}" }
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
