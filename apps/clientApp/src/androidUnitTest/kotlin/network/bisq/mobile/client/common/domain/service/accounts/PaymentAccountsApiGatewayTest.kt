package network.bisq.mobile.client.common.domain.service.accounts

import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountDto
import network.bisq.mobile.data.model.account.fiat.UserDefinedFiatAccountPayloadDto
import network.bisq.mobile.data.utils.encodeURIParam
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentAccountsApiGatewayTest {
    private val webSocketClientService: WebSocketClientService = mockk()
    private val webSocketApiClient = WebSocketApiClient(webSocketClientService, Json)
    private val gateway = PaymentAccountsApiGateway(webSocketApiClient)

    @After
    fun tearDown() {
        unmockkStatic(::encodeURIParam)
    }

    @Test
    fun `when getPaymentAccounts then delegates GET request to payment accounts endpoint`() =
        runTest {
            // Given
            val requestSlot = slot<WebSocketRestApiRequest>()
            coEvery {
                webSocketClientService.sendRequestAndAwaitResponse(capture(requestSlot))
            } returns WebSocketRestApiResponse(requestId = "request-1", statusCode = HttpStatusCode.OK.value, body = "[]")

            // When
            val result = gateway.getPaymentAccounts()

            // Then
            assertTrue(result.isSuccess)
            assertEquals("GET", requestSlot.captured.method)
            assertEquals("/api/v1/payment-accounts", requestSlot.captured.path)
            coVerify(exactly = 1) { webSocketClientService.sendRequestAndAwaitResponse(any()) }
        }

    @Test
    fun `when addAccount then delegates POST request to payment accounts endpoint`() =
        runTest {
            // Given
            val requestSlot = slot<WebSocketRestApiRequest>()
            coEvery {
                webSocketClientService.sendRequestAndAwaitResponse(capture(requestSlot))
            } returns
                WebSocketRestApiResponse(
                    requestId = "request-1",
                    statusCode = HttpStatusCode.OK.value,
                    body =
                        """
                        {
                          "accountName": "Account One",
                          "accountPayload": {"accountData": "alice@example.com"},
                          "paymentRail": "CUSTOM",
                          "tradeLimitInfo": null,
                          "tradeDuration": null,
                          "creationDate": null
                        }
                        """.trimIndent(),
                )
            val account = sampleAccountDto(accountName = "Account One", accountData = "alice@example.com")

            // When
            val result = gateway.addAccount(account)

            // Then
            assertTrue(result.isSuccess)
            assertEquals("POST", requestSlot.captured.method)
            assertEquals("/api/v1/payment-accounts", requestSlot.captured.path)
            assertTrue(requestSlot.captured.body.contains("\"accountName\":\"Account One\""))
            assertTrue(requestSlot.captured.body.contains("\"accountData\":\"alice@example.com\""))
        }

    @Test
    fun `when deleteAccount with spaces then encodes account name in query param`() =
        runTest {
            // Given
            mockkStatic(::encodeURIParam)
            every { encodeURIParam("My Account 1") } returns "My%20Account%201"
            val requestSlot = slot<WebSocketRestApiRequest>()
            coEvery {
                webSocketClientService.sendRequestAndAwaitResponse(capture(requestSlot))
            } returns WebSocketRestApiResponse(requestId = "request-1", statusCode = HttpStatusCode.NoContent.value, body = "")

            // When
            val result = gateway.deleteAccount("My Account 1")

            // Then
            assertTrue(result.isSuccess)
            assertEquals("DELETE", requestSlot.captured.method)
            assertEquals("/api/v1/payment-accounts?accountName=My%20Account%201", requestSlot.captured.path)
        }

    @Test
    fun `when saveAccount then delegates PUT request with wrapped account payload`() =
        runTest {
            // Given
            mockkStatic(::encodeURIParam)
            every { encodeURIParam("Saved Account") } returns "Saved%20Account"
            val requestSlot = slot<WebSocketRestApiRequest>()
            coEvery {
                webSocketClientService.sendRequestAndAwaitResponse(capture(requestSlot))
            } returns WebSocketRestApiResponse(requestId = "request-1", statusCode = HttpStatusCode.NoContent.value, body = "")
            val account = sampleAccountDto(accountName = "Saved Account", accountData = "save@example.com")

            // When
            val result = gateway.saveAccount("Saved Account", account)

            // Then
            assertTrue(result.isSuccess)
            assertEquals("PUT", requestSlot.captured.method)
            assertEquals("/api/v1/payment-accounts?accountName=Saved%20Account", requestSlot.captured.path)
            assertTrue(requestSlot.captured.body.contains("\"account\":"))
            assertTrue(requestSlot.captured.body.contains("\"accountName\":\"Saved Account\""))
            assertTrue(requestSlot.captured.body.contains("\"accountData\":\"save@example.com\""))
        }

    @Test
    fun `when getFiatPaymentMethods then delegates GET request to fiat payment methods endpoint`() =
        runTest {
            // Given
            val requestSlot = slot<WebSocketRestApiRequest>()
            coEvery {
                webSocketClientService.sendRequestAndAwaitResponse(capture(requestSlot))
            } returns WebSocketRestApiResponse(requestId = "request-1", statusCode = HttpStatusCode.OK.value, body = "[]")

            // When
            val result = gateway.getFiatPaymentMethods()

            // Then
            assertTrue(result.isSuccess)
            assertEquals("GET", requestSlot.captured.method)
            assertEquals("/api/v1/payment-accounts/payment-methods/fiat", requestSlot.captured.path)
        }

    @Test
    fun `when getCryptoPaymentMethods then delegates GET request to crypto payment methods endpoint`() =
        runTest {
            // Given
            val requestSlot = slot<WebSocketRestApiRequest>()
            coEvery {
                webSocketClientService.sendRequestAndAwaitResponse(capture(requestSlot))
            } returns WebSocketRestApiResponse(requestId = "request-1", statusCode = HttpStatusCode.OK.value, body = "[]")

            // When
            val result = gateway.getCryptoPaymentMethods()

            // Then
            assertTrue(result.isSuccess)
            assertEquals("GET", requestSlot.captured.method)
            assertEquals("/api/v1/payment-accounts/payment-methods/crypto", requestSlot.captured.path)
        }

    private fun sampleAccountDto(
        accountName: String,
        accountData: String,
    ): UserDefinedFiatAccountDto =
        UserDefinedFiatAccountDto(
            accountName = accountName,
            accountPayload = UserDefinedFiatAccountPayloadDto(accountData = accountData),
            tradeLimitInfo = null,
            tradeDuration = null,
            creationDate = null,
        )
}
