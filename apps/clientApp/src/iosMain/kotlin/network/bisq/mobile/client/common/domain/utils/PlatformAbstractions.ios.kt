@file:OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)

package network.bisq.mobile.client.common.domain.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.KtorNSURLSessionDelegate
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyConfig
import network.bisq.mobile.data.crypto.getSha256
import network.bisq.mobile.domain.utils.base64ToByteArray
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.domain.utils.toByteArray
import platform.Foundation.CFBridgingRelease
import platform.Foundation.NSData
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSession
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.NSURLSessionConfiguration
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecCertificateCopyData
import platform.Security.SecTrustGetCertificateAtIndex

/** File-level logger for TLS challenge handler to avoid allocation per invocation */
private val tlsLog = getLogger("TlsFingerprint")

/** Diagnostic logger for the platform abstraction. */
private val platformLog = getLogger("PlatformAbstractions.ios")

/**
 * Registry of `HttpClient → NSURLSession` so we can call `invalidateAndCancel`
 * on the underlying iOS session at dispose-time. Ktor 3.4.3's Darwin engine
 * keeps its `NSURLSession` `private` and closes with `finishTasksAndInvalidate`,
 * which lets zombie tasks linger up to 120s and pollutes the next session's
 * connection pool. We work around this by pre-creating the `NSURLSession`
 * ourselves and handing it to the engine via `usePreconfiguredSession`.
 */
private val sessionRegistry: MutableMap<HttpClient, NSURLSession> = mutableMapOf()
private val sessionRegistryLock = SynchronizedObject()

private fun registerSession(
    client: HttpClient,
    session: NSURLSession,
) {
    synchronized(sessionRegistryLock) {
        sessionRegistry[client] = session
    }
}

actual fun HttpClient.invalidateUnderlyingSession() {
    val session =
        synchronized(sessionRegistryLock) {
            sessionRegistry.remove(this)
        }
    if (session == null) {
        platformLog.d { "invalidateUnderlyingSession: no NSURLSession tracked for this HttpClient" }
        return
    }
    platformLog.i { "Invalidating NSURLSession (invalidateAndCancel) to drain zombie tasks" }
    session.invalidateAndCancel()
}

actual fun HttpClient.releaseUnderlyingSessionTracking() {
    val removed =
        synchronized(sessionRegistryLock) {
            sessionRegistry.remove(this) != null
        }
    if (!removed) {
        platformLog.d { "releaseUnderlyingSessionTracking: no NSURLSession tracked for this HttpClient" }
        return
    }
    // Do NOT call invalidateAndCancel here — leave the NSURLSession alone so
    // Ktor's HttpClient.close() can finishTasksAndInvalidate it, allowing any
    // in-flight WebSocket task to drain its close frame.
    platformLog.d { "Released NSURLSession tracking (close() will finishTasksAndInvalidate gracefully)" }
}

actual fun createHttpClient(
    host: String,
    tlsFingerprint: String?,
    proxyConfig: BisqProxyConfig?,
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient {
    // Build the NSURLSession ourselves so we hold the only Kotlin-side reference
    // and can invalidateAndCancel later. Mirrors Ktor's own DarwinSession.createSession
    // wiring (default config, no cookie storage, proxy via connectionProxyDictionary).
    val challengeHandler = tlsFingerprint?.let { fingerprint -> buildTlsChallengeHandler(fingerprint) }
    val delegate = KtorNSURLSessionDelegate(challengeHandler)

    val sessionConfiguration =
        NSURLSessionConfiguration.defaultSessionConfiguration().apply {
            setHTTPCookieStorage(null)
            proxyConfig?.config?.let { applyProxy(it) }
        }

    val session =
        NSURLSession.sessionWithConfiguration(
            configuration = sessionConfiguration,
            delegate = delegate,
            delegateQueue = NSOperationQueue(),
        )

    val client =
        HttpClient(Darwin) {
            config(this)
            install(WebSockets) {
                pingIntervalMillis = 15_000 // not supported by okhttp engine
            }
            engine {
                usePreconfiguredSession(session, delegate)
            }
        }
    registerSession(client, session)
    return client
}

/**
 * Mirrors Ktor's [`io.ktor.client.engine.darwin.setupProxy`] (internal API) so we can
 * configure proxy parameters on the [NSURLSessionConfiguration] we own. Kept in sync
 * with the Ktor source — if upstream gains a new protocol, update here.
 */
private fun NSURLSessionConfiguration.applyProxy(proxy: io.ktor.client.engine.ProxyConfig) {
    val url: Url = proxy.url
    when (url.protocol) {
        URLProtocol.HTTP, URLProtocol.HTTPS ->
            connectionProxyDictionary =
                mapOf<Any?, Any?>(
                    "HTTPEnable" to 1,
                    "HTTPProxy" to url.host,
                    "HTTPPort" to url.port,
                )
        URLProtocol.SOCKS ->
            connectionProxyDictionary =
                mapOf<Any?, Any?>(
                    "SOCKSEnable" to 1,
                    "SOCKSProxy" to url.host,
                    "SOCKSPort" to url.port,
                )
        else -> error("Proxy type ${url.protocol.name} is unsupported by Darwin client engine.")
    }
}

/**
 * Builds the Ktor [`ChallengeHandler`](io.ktor.client.engine.darwin.ChallengeHandler)
 * that delegates to [handleTlsChallenge]. Extracted so [createHttpClient] stays focused
 * on session wiring.
 */
private fun buildTlsChallengeHandler(
    fingerprint: String,
): (
    NSURLSession,
    platform.Foundation.NSURLSessionTask,
    NSURLAuthenticationChallenge,
    (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
) -> Unit =
    { _, _, challenge, completionHandler ->
        handleTlsChallenge(fingerprint, challenge, completionHandler)
    }

/**
 * Custom TLS challenge handler that validates the server certificate by comparing
 * the SHA-256 hash of the full DER-encoded certificate against the expected fingerprint.
 *
 * This matches Android's TlsTrustManager behaviour (which also hashes the full DER cert),
 * unlike Ktor's built-in CertificatePinner which hashes only the SPKI.
 *
 * Self-signed certificates are accepted when the fingerprint matches — no system
 * trust evaluation is performed, since fingerprint pinning IS the trust anchor.
 */
private fun handleTlsChallenge(
    expectedFingerprint: String,
    challenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
) {
    val protectionSpace = challenge.protectionSpace

    if (protectionSpace.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
        completionHandler(NSURLSessionAuthChallengePerformDefaultHandling, null)
        return
    }

    val serverTrust = protectionSpace.serverTrust
    if (serverTrust == null) {
        tlsLog.e { "No server trust available" }
        completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
        return
    }

    // Use deprecated API with suppression - SecTrustCopyCertificateChain requires complex CFArray bridging in K/N
    @Suppress("DEPRECATION")
    val cert = SecTrustGetCertificateAtIndex(serverTrust, 0)
    if (cert == null) {
        tlsLog.e { "No leaf certificate in trust chain" }
        completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
        return
    }

    val certDataRef = SecCertificateCopyData(cert)
    if (certDataRef == null) {
        tlsLog.e { "Failed to get DER data from certificate" }
        completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
        return
    }

    try {
        // Get full DER-encoded certificate bytes (toll-free bridged CFData -> NSData)
        @Suppress("UNCHECKED_CAST")
        val nsData = CFBridgingRelease(certDataRef) as NSData
        val derBytes = nsData.toByteArray()

        // SHA-256 hash of full DER certificate (matching Android's TlsTrustManager)
        val hash = getSha256(derBytes)

        // Base64 decode the expected fingerprint (same format as Android)
        val expectedHash =
            expectedFingerprint.base64ToByteArray()
                ?: throw IllegalStateException("Invalid Base64 fingerprint: $expectedFingerprint")

        if (hash.contentEquals(expectedHash)) {
            val credential = NSURLCredential.credentialForTrust(serverTrust)
            completionHandler(NSURLSessionAuthChallengeUseCredential, credential)
        } else {
            tlsLog.e { "TLS fingerprint verification failed" }
            completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
        }
    } catch (e: Exception) {
        tlsLog.e { "TLS trust check failed: ${e.message}" }
        completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge, null)
    }
}
