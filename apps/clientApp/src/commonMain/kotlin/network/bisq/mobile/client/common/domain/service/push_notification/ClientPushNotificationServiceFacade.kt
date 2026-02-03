package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.push_notification.PushNotificationServiceFacade
import network.bisq.mobile.domain.utils.Logging

/**
 * Client implementation of PushNotificationServiceFacade.
 * Manages device token registration with the trusted node.
 *
 * - Uses a deterministic device-specific deviceId based on hardware identifiers
 *   (Android: ANDROID_ID, iOS: identifierForVendor)
 * - Includes deviceDescriptor for device information
 * - Multi-profile safe: deviceId is per-device, not per-profile
 */
class ClientPushNotificationServiceFacade(
    private val apiGateway: PushNotificationApiGateway,
    private val settingsRepository: SettingsRepository,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val pushNotificationTokenProvider: PushNotificationTokenProvider,
    private val userProfileServiceFacade: network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade,
) : ServiceFacade(),
    PushNotificationServiceFacade,
    Logging {
    private val _isPushNotificationsEnabled = MutableStateFlow(false)
    override val isPushNotificationsEnabled: StateFlow<Boolean> = _isPushNotificationsEnabled.asStateFlow()

    private val _isDeviceRegistered = MutableStateFlow(false)
    override val isDeviceRegistered: StateFlow<Boolean> = _isDeviceRegistered.asStateFlow()

    private val _deviceToken = MutableStateFlow<String?>(null)
    override val deviceToken: StateFlow<String?> = _deviceToken.asStateFlow()

    private val _deviceId = MutableStateFlow<String?>(null)
    private val deviceId: StateFlow<String?> = _deviceId.asStateFlow()

    override suspend fun activate() {
        super<ServiceFacade>.activate()

        log.i { "Activating native push notification service" }

        // Load saved push notification preference
        serviceScope.launch {
            settingsRepository.data.collect { settings ->
                val wasEnabled = _isPushNotificationsEnabled.value
                _isPushNotificationsEnabled.value = settings.pushNotificationsEnabled

                // Only auto-register if:
                // 1. Push notifications are enabled in settings
                // 2. Device is not already registered
                // 3. User has completed onboarding (has a trusted node configured)
                if (settings.pushNotificationsEnabled && !_isDeviceRegistered.value && !wasEnabled) {
                    tryAutoRegisterIfOnboarded()
                }
            }
        }
    }

    /**
     * Attempts to auto-register only if the user has completed onboarding.
     * This prevents prompting for notifications during the initial setup flow.
     */
    private suspend fun tryAutoRegisterIfOnboarded() {
        try {
            // Check if user has completed onboarding by checking if they have a trusted node configured
            val sensitiveSettings = sensitiveSettingsRepository.fetch()
            if (sensitiveSettings.bisqApiUrl.isBlank()) {
                log.d { "Skipping auto-registration - user has not completed onboarding yet" }
                return
            }

            log.i { "Push notifications enabled and onboarding complete - auto-registering device" }
            val result = registerForPushNotifications()
            if (result.isSuccess) {
                log.i { "Auto-registration successful" }
            } else {
                log.w { "Auto-registration failed: ${result.exceptionOrNull()?.message}" }
            }
        } catch (e: Exception) {
            log.e(e) { "Error during auto-registration" }
        }
    }

    override suspend fun requestPermission(): Boolean = pushNotificationTokenProvider.requestPermission()

    override suspend fun registerForPushNotifications(): Result<Unit> {
        log.i { "Registering for push notifications..." }

        // First, request permission
        val hasPermission = requestPermission()
        if (!hasPermission) {
            log.w { "Push notification permission denied" }
            return Result.failure(PushNotificationException("Permission denied"))
        }

        // Request device token from platform
        val tokenResult = pushNotificationTokenProvider.requestDeviceToken()
        if (tokenResult.isFailure) {
            log.e { "Failed to get device token: ${tokenResult.exceptionOrNull()?.message}" }
            return Result.failure(tokenResult.exceptionOrNull() ?: PushNotificationException("Failed to get device token"))
        }

        val token = tokenResult.getOrNull()
        if (token.isNullOrBlank()) {
            log.e { "Device token is null or blank" }
            return Result.failure(PushNotificationException("Device token is null or blank"))
        }

        _deviceToken.value = token
        log.i { "Got device token: ${token.take(10)}..." }

        // Register with trusted node
        return registerTokenWithTrustedNode(token)
    }

    private suspend fun registerTokenWithTrustedNode(token: String): Result<Unit> {
        // Get the current user profile (needed for publicKeyBase64)
        val userProfile = userProfileServiceFacade.selectedUserProfile.value
        if (userProfile == null) {
            log.e { "Cannot register device: no user profile selected" }
            return Result.failure(PushNotificationException("No user profile selected"))
        }

        // publicKey.encoded is already a base64-encoded String from Bisq2
        val publicKeyBase64 = userProfile.networkId.pubKey.publicKey.encoded

        // Get deterministic device-specific deviceId (based on hardware identifiers)
        val deviceId = getDeviceId()
        _deviceId.value = deviceId

        // Get device descriptor and platform dynamically
        val platformInfo = getPlatformInfo()
        val deviceDescriptor = platformInfo.name
        val platform = PlatformMapper.fromPlatformType(platformInfo.type)

        log.i { "Registering device with deviceId: $deviceId, descriptor: $deviceDescriptor, platform: $platform" }

        val result =
            apiGateway.registerDevice(
                deviceId = deviceId,
                deviceToken = token,
                publicKeyBase64 = publicKeyBase64,
                deviceDescriptor = deviceDescriptor,
                platform = platform,
            )
        if (result.isSuccess) {
            log.i { "Device registered successfully with trusted node" }
            _isDeviceRegistered.value = true
            settingsRepository.update { it.copy(pushNotificationsEnabled = true) }
        } else {
            log.e { "Failed to register device with trusted node: ${result.exceptionOrNull()?.message}" }
        }
        return result
    }

    override suspend fun unregisterFromPushNotifications(): Result<Unit> {
        log.i { "Unregistering from push notifications..." }

        // Get deterministic device-specific deviceId (always available from hardware)
        val deviceIdToUnregister = getDeviceId()

        val result = apiGateway.unregisterDevice(deviceIdToUnregister)
        // Always update local state regardless of API result
        _isDeviceRegistered.value = false
        _deviceId.value = null
        settingsRepository.update { it.copy(pushNotificationsEnabled = false) }

        if (result.isSuccess) {
            log.i { "Device unregistered successfully" }
        } else {
            log.e { "Failed to unregister device from server: ${result.exceptionOrNull()?.message}" }
        }
        return result
    }

    override suspend fun onDeviceTokenReceived(token: String) {
        log.i { "Device token received: ${token.take(10)}..." }
        _deviceToken.update { token }

        // If push notifications are enabled, re-register with new token
        if (_isPushNotificationsEnabled.value) {
            val result = registerTokenWithTrustedNode(token)
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                log.e(error) { "Failed to re-register device with new token" }
                // Update registration state to reflect failure
                _isDeviceRegistered.value = false
            }
        }
    }

    override suspend fun onDeviceTokenRegistrationFailed(error: Throwable) {
        log.e(error) { "Device token registration failed" }
        _deviceToken.value = null
    }
}

class PushNotificationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
