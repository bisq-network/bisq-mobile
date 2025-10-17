package network.bisq.mobile.android.node

import android.content.res.Configuration
import android.os.Build
import android.os.Process
import bisq.common.facades.FacadeProvider
import bisq.common.facades.android.AndroidGuavaFacade
import bisq.common.facades.android.AndroidJdkFacade
import bisq.common.network.clear_net_address_types.AndroidEmulatorAddressTypeFacade
import bisq.common.network.clear_net_address_types.LANAddressTypeFacade
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.android.node.di.androidNodeModule
import network.bisq.mobile.android.node.di.serviceModule
import network.bisq.mobile.android.node.presentation.backupFileName
import network.bisq.mobile.android.node.utils.moveDirReplace
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.presentation.MainApplication
import network.bisq.mobile.presentation.di.presentationModule
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.koin.android.ext.android.get
import org.koin.core.module.Module
import java.io.File
import java.security.Security

/**
 * Bisq Android Node Application definition
 */
class NodeMainApplication : MainApplication() {

    override fun getKoinModules(): List<Module> {
        return listOf(domainModule, serviceModule, presentationModule, androidNodeModule)
    }

    override fun onCreated() {
        // Use runBlocking for essential system initialization that must complete before app continues
        // This is acceptable here because:
        // 1. It's Application.onCreate() - the right place for critical setup
        // 2. setupBisqCoreStatics() configures essential system components (BouncyCastle, Facades)
        // 3. The app cannot function without these being initialized
        // 4. It's a one-time operation during app startup
        runBlocking(IODispatcher) {
            setupBisqCoreStatics()
        }

        maybeRestoreDataDirectory()

        // We start here the initialisation (non blocking) of the core services and tor.
        // The lifecycle of those is tied to the lifecycle of the Application/Process not to the lifecycle of the MainActivity.
        // As Android does not provide any callback when the process gets terminated we cannot gracefully shutdown the services and tor.
        // Only if the user shutdown or restart we can do that.
        val nodeApplicationLifecycleService: NodeApplicationLifecycleService = get()
        nodeApplicationLifecycleService.initialize()

        log.i { "Bisq Easy Node Application Created" }
    }

    private fun maybeRestoreDataDirectory() {
        val backupDir = File(filesDir, backupFileName)
        if (backupDir.exists()) {
            log.i { "Restore from backup" }
            val dbDir = File(filesDir, "Bisq2_mobile/db")

            val backupPrivate = File(backupDir, "private")
            val targetPrivate = File(dbDir, "private")
            val backupSettings = File(backupDir, "settings")
            val targetSettings = File(dbDir, "settings")

            var privateMoved = false
            var settingsMoved = false
            try {
                moveDirReplace(backupPrivate, targetPrivate)
                privateMoved = true
                moveDirReplace(backupSettings, targetSettings)
                settingsMoved = true

                if (backupDir.deleteRecursively()) {
                    log.i { "We restored successfully from a backup" }
                } else {
                    log.w { "Could not delete backup dir at restore from backup" }
                }
            } catch (e: Exception) {
                log.w(e) { "Restore from backup failed; attempting rollback" }
                // Rollback to keep backup intact for future retries
                if (settingsMoved) {
                    runCatching { moveDirReplace(targetSettings, backupSettings) }
                        .onFailure { ex -> log.w(ex) { "Rollback settings failed" } }
                }
                if (privateMoved) {
                    runCatching { moveDirReplace(targetPrivate, backupPrivate) }
                        .onFailure { ex -> log.w(ex) { "Rollback private failed" } }
                }
                log.w { "Restore incomplete; keeping backup dir at ${backupDir.absolutePath}" }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // do nth - Required by ComponentCallbacks2 interface
    }

    override fun isDebug(): Boolean {
        return BuildNodeConfig.IS_DEBUG
    }

    private fun setupBisqCoreStatics() {
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

    private fun isEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
