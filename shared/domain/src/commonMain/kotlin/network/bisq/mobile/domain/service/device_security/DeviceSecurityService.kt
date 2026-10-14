package network.bisq.mobile.domain.service.device_security

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.Logging

/**
 * Owns the rooted / jailbroken device warning state shared by the dashboard and Settings.
 *
 * The detection result is kept in memory only and is never logged, sent to analytics or attached
 * to crash reports: it is a device fingerprint. What is persisted is the baseline (for the wording)
 * and the "Don't show again" decision.
 */
class DeviceSecurityService(
    private val probe: DeviceIntegrityProbe,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : Logging {
    private val _integrity = MutableStateFlow<DeviceIntegrity?>(null)

    /** Latest detection result, null until the first check of this process. */
    val integrity: StateFlow<DeviceIntegrity?> = _integrity.asStateFlow()

    private val mutex = Mutex()

    /** Runs the probe, persists the outcome and tells the dashboard whether the warning is pending. */
    suspend fun onDashboardArrival(): DeviceSecurityResolution =
        mutex.withLock {
            val detected = detect()
            val settings = settingsRepository.fetch()
            if (detected == null) {
                // A failed probe must neither produce a warning nor overwrite what was recorded.
                _integrity.value = DeviceIntegrity.CLEAN
                return@withLock DeviceSecurityResolution(
                    baseline = settings.deviceSecurityBaseline,
                    suppressed = settings.deviceSecurityWarningSuppressed,
                    isRooted = false,
                    isPending = false,
                    wording = null,
                )
            }
            _integrity.value = detected
            val resolution =
                resolveDeviceSecurity(
                    integrity = detected,
                    baseline = settings.deviceSecurityBaseline,
                    suppressed = settings.deviceSecurityWarningSuppressed,
                )
            if (resolution.baseline != settings.deviceSecurityBaseline ||
                resolution.suppressed != settings.deviceSecurityWarningSuppressed
            ) {
                settingsRepository.update {
                    it.copy(
                        deviceSecurityBaseline = resolution.baseline,
                        deviceSecurityWarningSuppressed = resolution.suppressed,
                    )
                }
            }
            resolution
        }

    /** Detects once per process for screens reached without passing the dashboard first. */
    suspend fun ensureChecked() {
        mutex.withLock {
            if (_integrity.value == null) {
                _integrity.value = detect() ?: DeviceIntegrity.CLEAN
            }
        }
    }

    /** The explicit "Don't show again" decision. */
    suspend fun suppressWarning() {
        settingsRepository.update { it.copy(deviceSecurityWarningSuppressed = true) }
    }

    /** Part of resetting all "Don't show again" flags. */
    suspend fun clearSuppression() {
        settingsRepository.update { it.copy(deviceSecurityWarningSuppressed = false) }
    }

    private suspend fun detect(): DeviceIntegrity? =
        try {
            withContext(ioDispatcher) { probe.detect() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.w(e) { "Device integrity check failed; treating the device as clean" }
            null
        }
}
