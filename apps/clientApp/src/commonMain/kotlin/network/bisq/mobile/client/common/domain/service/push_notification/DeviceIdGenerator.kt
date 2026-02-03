package network.bisq.mobile.client.common.domain.service.push_notification

/**
 * Returns a deterministic, device-specific ID for push notifications.
 *
 * The deviceId is:
 * - Device-specific (not profile-specific) to support multi-profile scenarios
 * - Deterministic (same device always returns same ID)
 * - Persistent across app restarts (based on hardware/system identifiers)
 *
 * Platform implementations:
 * - iOS: Uses UIDevice.identifierForVendor (persists for same vendor apps)
 * - Android: Uses Settings.Secure.ANDROID_ID (persists across app reinstalls)
 */
expect fun getDeviceId(): String
