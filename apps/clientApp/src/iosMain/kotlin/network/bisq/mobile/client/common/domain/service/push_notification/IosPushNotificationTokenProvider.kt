package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import network.bisq.mobile.domain.utils.Logging
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

/**
 * iOS implementation of PushNotificationTokenProvider.
 * Registers with APNs to get a device token.
 *
 * Note: The actual registration for remote notifications must be triggered from Swift
 * (AppDelegate) because UIApplication.registerForRemoteNotifications() is not directly
 * accessible from Kotlin/Native. This class provides the callback mechanism to receive
 * the token once it's available.
 */
class IosPushNotificationTokenProvider :
    PushNotificationTokenProvider,
    Logging {
    companion object {
        // Singleton to hold the pending token request
        // This is needed because the token is delivered asynchronously via AppDelegate
        private var pendingTokenRequest: CompletableDeferred<String>? = null

        // Flag to signal that registration should be triggered
        private var shouldRegister = false

        // Mutex for thread-safe access to pendingTokenRequest
        private val mutex = Mutex()

        /**
         * Called from AppDelegate when a device token is received.
         */
        fun onTokenReceived(token: String) {
            pendingTokenRequest?.complete(token)
            pendingTokenRequest = null
        }

        /**
         * Called from AppDelegate when token registration fails.
         */
        fun onTokenRegistrationFailed(error: Throwable) {
            pendingTokenRequest?.completeExceptionally(error)
            pendingTokenRequest = null
        }

        /**
         * Check if registration should be triggered.
         * Called from Swift to determine if registerForRemoteNotifications should be called.
         */
        fun shouldTriggerRegistration(): Boolean {
            val result = shouldRegister
            shouldRegister = false
            return result
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun requestPermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            val center = UNUserNotificationCenter.currentNotificationCenter()
            val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge

            center.requestAuthorizationWithOptions(options) { granted, error ->
                if (error != null) {
                    log.e { "Failed to request notification permission: ${error.localizedDescription}" }
                    continuation.resume(false)
                } else {
                    log.i { "Notification permission granted: $granted" }
                    continuation.resume(granted)
                }
            }
        }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun requestDeviceToken(): Result<String> {
        // Check if we already have permission
        val hasPermission = checkPermission()
        if (!hasPermission) {
            return Result.failure(PushNotificationException("Notification permission not granted"))
        }

        // Reuse existing pending request if present and not completed
        val deferred =
            mutex.withLock {
                pendingTokenRequest?.takeIf { !it.isCompleted } ?: CompletableDeferred<String>().also {
                    pendingTokenRequest = it
                    shouldRegister = true // Only trigger registration for new requests
                    log.i { "Requesting device token registration..." }
                }
            }

        // Wait for the token with a timeout (will be delivered via AppDelegate)
        return try {
            val token = withTimeout(30_000L) { deferred.await() }
            log.i { "Received device token: ${token.take(10)}..." }
            Result.success(token)
        } catch (e: CancellationException) {
            // Preserve structured concurrency - rethrow cancellation
            throw e
        } catch (e: Exception) {
            // Clear pending request on error
            mutex.withLock {
                pendingTokenRequest = null
            }
            log.e(e) { "Failed to get device token" }
            Result.failure(e)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun checkPermission(): Boolean =
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter
                .currentNotificationCenter()
                .getNotificationSettingsWithCompletionHandler { settings ->
                    val isAuthorized = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
                    continuation.resume(isAuthorized)
                }
        }
}
