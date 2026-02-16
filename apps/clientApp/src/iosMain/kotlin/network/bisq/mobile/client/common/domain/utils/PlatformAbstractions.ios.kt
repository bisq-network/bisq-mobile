@file:OptIn(ExperimentalForeignApi::class)

package network.bisq.mobile.client.common.domain.utils

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.cinterop.ExperimentalForeignApi
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyConfig
import network.bisq.mobile.crypto.getSha256
import network.bisq.mobile.domain.utils.base64ToByteArray
import network.bisq.mobile.domain.utils.getLogger
import network.bisq.mobile.ios.toByteArray
import platform.Foundation.*
import platform.Security.*

/** File-level logger for TLS challenge handler to avoid allocation per invocation */
private val tlsLog = getLogger("TlsFingerprint")

actual fun createHttpClient(
    host: String,
    tlsFingerprint: String?,
    proxyConfig: BisqProxyConfig?,
    config: HttpClientConfig<*>.() -> Unit,
): HttpClient =
    HttpClient(Darwin) {
        config(this)
        install(WebSockets) {
            pingIntervalMillis = 15_000 // not supported by okhttp engine
        }
        engine {
            proxy = proxyConfig?.config

            tlsFingerprint?.let { fingerprint ->
                handleChallenge { _, _, challenge, completionHandler ->
                    handleTlsChallenge(fingerprint, challenge, completionHandler)
                }
            }
        }
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
