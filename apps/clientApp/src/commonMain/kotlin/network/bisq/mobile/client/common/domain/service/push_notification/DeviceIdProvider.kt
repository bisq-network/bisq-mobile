package network.bisq.mobile.client.common.domain.service.push_notification

/**
 * Provides the platform device identifier.
 *
 * Abstracted behind an interface so the transient-failure path can be exercised in tests: on iOS
 * `UIDevice.identifierForVendor` is nil right after a device restart, before the first unlock (see
 * [getDeviceId] actual). Implementations may therefore throw [IllegalStateException] when the
 * identifier is temporarily unavailable — callers must treat that as a recoverable failure, not a
 * crash. The production default simply delegates to the platform [getDeviceId] expect/actual.
 */
fun interface DeviceIdProvider {
    fun getDeviceId(): String
}
