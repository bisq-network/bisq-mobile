package network.bisq.mobile.domain.service.device_security

/** Result of the local root (Android) or jailbreak (iOS) heuristics. */
enum class DeviceIntegrity {
    CLEAN,
    ROOTED,
}

/**
 * Local heuristics only: no attestation service and nothing leaves the device. A false negative is
 * acceptable, a false positive is not, so an indicator that cannot be read counts as absent.
 */
interface DeviceIntegrityProbe {
    fun detect(): DeviceIntegrity
}

expect fun createDeviceIntegrityProbe(): DeviceIntegrityProbe
