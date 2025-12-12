package network.bisq.mobile.node.main

import android.content.res.Configuration
import android.os.Build
import android.os.Process
import bisq.common.facades.FacadeProvider
import bisq.common.facades.android.AndroidGuavaFacade
import bisq.common.facades.android.AndroidJdkFacade
import bisq.common.network.clear_net_address_types.AndroidEmulatorAddressTypeFacade
import bisq.common.network.clear_net_address_types.LANAddressTypeFacade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import network.bisq.mobile.android.node.BuildNodeConfig
import network.bisq.mobile.domain.di.domainModule
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.node.common.di.androidNodeDomainModule
import network.bisq.mobile.node.common.di.androidNodePresentationModule
import network.bisq.mobile.node.settings.resources.backupFileName
import network.bisq.mobile.node.common.domain.utils.moveDirReplace
import network.bisq.mobile.presentation.main.MainApplication
import network.bisq.mobile.presentation.common.di.presentationModule
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
        return listOf(domainModule, androidNodeDomainModule, presentationModule, androidNodePresentationModule)
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

        maybeRestoreDataDirectory()
        maybeCleanupCorruptedChatData()

        // We start here the initialisation (non blocking) of the core services and tor.
        // The lifecycle of those is tied to the lifecycle of the Application/Process not to the lifecycle of the MainActivity.
        // As Android does not provide any callback when the process gets terminated we cannot gracefully shutdown the services and tor.
        // Only if the user shutdown or restart we can do that.
        val nodeApplicationLifecycleService: ApplicationLifecycleService = get()
        nodeApplicationLifecycleService.initialize()

        log.i { "Bisq Easy Node Application Created" }
    }

    /**
     * Cleans up corrupted chat data from old bisq2 versions.
     *
     * This fixes crashes caused by deprecated SubDomain enum values (e.g., EVENTS_*)
     * that were removed in bisq2 2.1.7 but may still exist in persisted data.
     *
     * The crash occurs during ChatService initialization when it tries to deserialize
     * old CommonPublicChatChannel data containing deprecated enum values.
     *
     * Only deletes files that actually contain the problematic enum values to minimize data loss.
     */
    private fun maybeCleanupCorruptedChatData() {
        try {
            val dbDir = File(filesDir, "Bisq2_mobile/db")
            val cacheDir = File(dbDir, "cache")

            if (!cacheDir.exists()) {
                log.i { "Cache directory does not exist, skipping chat data cleanup" }
                return
            }

            // Deprecated SubDomain enum values that cause crashes
            val deprecatedEnumValues = listOf(
                "EVENTS_CONFERENCES",
                "EVENTS_MEETUPS",
                "EVENTS_PODCASTS",
                "EVENTS_TRADE_EVENTS",
                "ChatChannelDomain.EVENTS"  // The domain itself
            )

            // Only check public chat channel files (not private trade chats)
            val publicChatFiles = listOf(
                "CommonPublicChatChannelStore",
                "DiscussionChannelStore",
                "EventsChannelStore",
                "SupportChannelStore"
            )

            publicChatFiles.forEach { fileName ->
                val file = File(cacheDir, fileName)
                if (file.exists() && file.isFile) {
                    try {
                        // Check if file contains any deprecated enum values
                        val containsDeprecatedValue = file.readText(Charsets.UTF_8).let { content ->
                            deprecatedEnumValues.any { deprecated -> content.contains(deprecated) }
                        }

                        if (containsDeprecatedValue) {
                            val deleted = file.delete()
                            if (deleted) {
                                log.i { "Deleted corrupted chat data file: $fileName (contained deprecated SubDomain values)" }
                            } else {
                                log.w { "Failed to delete corrupted chat data file: $fileName" }
                            }
                        }
                    } catch (e: Exception) {
                        log.w(e) { "Error checking file $fileName, deleting it to be safe" }
                        val deleted = file.delete()
                        if (deleted) {
                            log.i { "Deleted unreadable chat data file: $fileName" }
                        } else {
                            log.w { "Failed to delete unreadable chat data file: $fileName" }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            log.w(e) { "Error during chat data cleanup - will attempt to continue" }
        }
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
