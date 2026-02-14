package network.bisq.mobile.client.common.domain.access

import kotlinx.datetime.Instant
import network.bisq.mobile.client.common.domain.access.pairing.PairingCode
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.access.utils.ApiAccessUtil.getProxyOptionFromRestUrl
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.domain.data.EnvironmentController
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.i18n.i18n

const val LOCALHOST = "localhost"

// Demo mode pairing code - when entered, triggers demo mode with fake data
const val DEMO_PAIRING_CODE = "BISQ_DEMO_PAIRING_CODE"

// Demo mode URLs that trigger WebSocketClientDemo
const val DEMO_API_URL = "http://demo.bisq:21"
const val DEMO_WS_URL = "ws://demo.bisq:21"

// Demo mode credentials - used for both in-memory state and persisted settings
private const val DEMO_CLIENT_ID = "demo-client-id"
private const val DEMO_CLIENT_SECRET = "demo-client-secret"
private const val DEMO_SESSION_ID = "demo-session-id"
private const val DEMO_PAIRING_ID = "demo-pairing-id"

class ApiAccessService(
    private val pairingService: PairingService,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val pairingQrCodeDecoder: PairingQrCodeDecoder,
    private val environmentController: EnvironmentController,
) : ServiceFacade(),
    Logging {
    fun getPairingCodeQr(value: String): Result<PairingQrCode> {
        val trimmedValue = value.trim()

        // Special case for demo mode - bypass validation
        if (trimmedValue == DEMO_PAIRING_CODE) {
            log.i { "Demo pairing code detected - returning demo PairingQrCode" }
            val demoPairingCode =
                PairingCode(
                    id = DEMO_PAIRING_ID,
                    expiresAt = Instant.DISTANT_FUTURE,
                    grantedPermissions = Permission.entries.toSet(),
                )
            val demoPairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode = demoPairingCode,
                    webSocketUrl = DEMO_WS_URL,
                    restApiUrl = DEMO_API_URL,
                    tlsFingerprint = null,
                    torClientAuthSecret = null,
                )
            return Result.success(demoPairingQrCode)
        }

        return try {
            val code = pairingQrCodeDecoder.decode(trimmedValue)
            Result.success(code)
        } catch (e: Exception) {
            log.e(e) { "Decoding pairing code failed." }
            Result.failure(Throwable("mobile.trustedNodeSetup.pairingCode.invalid".i18n()))
        }
    }

    suspend fun updateSettings(pairingQrCode: PairingQrCode) {
        val url = pairingQrCode.restApiUrl
        if (url.isNotBlank()) {
            sensitiveSettingsRepository.update {
                it.copy(
                    bisqApiUrl = pairingQrCode.restApiUrl,
                    selectedProxyOption = getProxyOptionFromRestUrl(url),
                    tlsFingerprint = pairingQrCode.tlsFingerprint,
                    // Clear old credentials so ensurePairingCredentials always
                    // requests fresh pairing when a new QR code is scanned.
                    // Old sessions may have expired or the server may have restarted.
                    clientId = null,
                    sessionId = null,
                    clientSecret = null,
                )
            }
        }
    }

    suspend fun requestPairing(
        pairingQrCode: PairingQrCode,
    ): Result<PairingResponse> {
        // Special case for demo mode - return fake credentials without HTTP request
        if (pairingQrCode.pairingCode.id == DEMO_PAIRING_ID) {
            log.i { "Demo mode detected - returning fake pairing response" }
            val demoResponse =
                PairingResponse(
                    version = 1,
                    clientId = DEMO_CLIENT_ID,
                    clientSecret = DEMO_CLIENT_SECRET,
                    sessionId = DEMO_SESSION_ID,
                    sessionExpiryDate = Long.MAX_VALUE,
                )
            ApplicationBootstrapFacade.isDemo = true
            sensitiveSettingsRepository.update {
                it.copy(
                    bisqApiUrl = DEMO_API_URL,
                    tlsFingerprint = null,
                    clientName = generateClientName(),
                    clientId = DEMO_CLIENT_ID,
                    sessionId = DEMO_SESSION_ID,
                    clientSecret = DEMO_CLIENT_SECRET,
                    selectedProxyOption = BisqProxyOption.NONE,
                )
            }
            return Result.success(demoResponse)
        }

        // Clear demo mode when a real pairing is requested
        if (ApplicationBootstrapFacade.isDemo) {
            log.i { "Real pairing requested - exiting demo mode" }
            ApplicationBootstrapFacade.isDemo = false
        }

        // Now we do a HTTP POST request for pairing.
        // This request is unauthenticated and will return the data we
        // need for establishing an authenticated and authorized
        // websocket connection.
        val clientName = generateClientName()
        return pairingService
            .requestPairing(
                pairingQrCode.pairingCode.id,
                clientName,
            ).onSuccess { pairingData ->
                log.i { "Pairing request was successful." }
                updatedSettings(
                    pairingData,
                    pairingQrCode,
                    clientName,
                )
            }.onFailure { error ->
                log.e(error) { "Pairing request failed." }
            }
    }

    private suspend fun updatedSettings(
        pairingResponse: PairingResponse,
        pairingQrCode: PairingQrCode,
        clientName: String,
    ) {
        sensitiveSettingsRepository.update {
            it.copy(
                clientId = pairingResponse.clientId,
                sessionId = pairingResponse.sessionId,
                clientSecret = pairingResponse.clientSecret,
                bisqApiUrl = pairingQrCode.restApiUrl,
                tlsFingerprint = pairingQrCode.tlsFingerprint,
                clientName = clientName,
            )
        }
    }

    private fun generateClientName(): String {
        val platformInfo = getPlatformInfo()
        return "Bisq Connect ${platformInfo.name}"
    }
}
