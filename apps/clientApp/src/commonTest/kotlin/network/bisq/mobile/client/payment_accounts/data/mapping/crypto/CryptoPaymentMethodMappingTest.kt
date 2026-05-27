package network.bisq.mobile.client.payment_accounts.data.mapping.crypto

import network.bisq.mobile.client.payment_accounts.data.model.crypto.CryptoPaymentMethodDto
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoPaymentMethodMappingTest {
    @Test
    fun `toDomain maps all CryptoPaymentMethod fields correctly`() {
        // Given
        val dto =
            CryptoPaymentMethodDto(
                code = "XMR",
                name = "Monero",
                supportAutoConf = true,
                tradeLimitInfo = "5000.00",
                tradeDuration = "4 days",
            )

        // When
        val result = dto.toDomain()

        // Then
        assertEquals("XMR", result.code)
        assertEquals("Monero", result.name)
        assertEquals(true, result.supportAutoConf)
        assertEquals("5000.00", result.tradeLimitInfo)
        assertEquals("4 days", result.tradeDuration)
    }
}
