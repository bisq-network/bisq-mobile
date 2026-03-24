package network.bisq.mobile.client.common.domain.service.push_notification

import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
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
 * Communication with Swift:
 * - Swift calls [setRegistrationTrigger] at app launch to provide a callback
 *   that invokes UIApplication.registerForRemoteNotifications().
 * - When Kotlin needs a token, [requestDeviceToken] invokes the callback directly.
 * - Swift's AppDelegate delivers the token back via [onTokenReceived] or
 *   reports failure via [onTokenRegistrationFailed].
 */
class IosPushNotificationTokenProvider :
    PushNotificationTokenProvider,
    Logging {
    companion object {
        // Atomic reference to hold the pending token request
        // This is needed because the token is delivered asynchronously via AppDelegate
        // Using atomic to ensure thread-safe access from both coroutines and Swift callbacks
        private val pendingTokenRequestRef = atomic<CompletableDeferred<String>?>(null)

        // Callback provided by Swift to trigger UIApplication.registerForRemoteNotifications()
        private val registrationTriggerRef = atomic<(() -> Unit)?>(null)

        // Mutex for thread-safe access when creating new deferred
        private val mutex = Mutex()

        /**
         * Called from Swift at app launch to provide the registration trigger callback.
         * The callback should call UIApplication.shared.registerForRemoteNotifications()
         * on the main thread.
         */
        fun setRegistrationTrigger(trigger: () -> Unit) {
            registrationTriggerRef.value = trigger
        }

        /**
         * Called from AppDelegate when a device token is received.
         * Thread-safe: uses atomic getAndSet to avoid race conditions.
         */
        fun onTokenReceived(token: String) {
            pendingTokenRequestRef.getAndSet(null)?.complete(token)
        }

        /**
         * Called from AppDelegate when token registration fails.
         * Thread-safe: uses atomic getAndSet to avoid race conditions.
         */
        fun onTokenRegistrationFailed(error: Throwable) {
            pendingTokenRequestRef.getAndSet(null)?.completeExceptionally(error)
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
                pendingTokenRequestRef.value?.takeIf { !it.isCompleted } ?: CompletableDeferred<String>().also {
                    pendingTokenRequestRef.value = it
                    // Trigger registration directly via the Swift callback
                    val trigger = registrationTriggerRef.value
                    if (trigger != null) {
                        log.i { "Requesting device token registration..." }
                        trigger()
                    } else {
                        log.e { "Registration trigger not set - Swift callback not registered" }
                        it.completeExceptionally(
                            PushNotificationException("Registration trigger not available. Ensure setRegistrationTrigger is called from Swift at app launch."),
                        )
                    }
                }
            }

        // Wait for the token with a timeout (will be delivered via AppDelegate)
        return try {
            val token = withTimeout(30_000L) { deferred.await() }
            log.i { "Received device token: ${token.take(10)}..." }
            Result.success(token)
        } catch (e: TimeoutCancellationException) {
            // Timeout is a specific case - clear pending request and return failure
            pendingTokenRequestRef.value = null
            log.e(e) { "Timeout waiting for device token" }
            Result.failure(e)
        } catch (e: CancellationException) {
            // Preserve structured concurrency - rethrow non-timeout cancellation
            throw e
        } catch (e: Exception) {
            // Clear pending request on error
            pendingTokenRequestRef.value = null
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
