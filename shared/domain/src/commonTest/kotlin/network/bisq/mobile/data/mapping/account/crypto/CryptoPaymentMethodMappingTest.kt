package network.bisq.mobile.data.mapping.account.crypto

import network.bisq.mobile.data.model.account.crypto.CryptoPaymentMethodDto
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
                category = "PRIVACY",
                supportAutoConf = true,
                tradeLimitInfo = "5000.00",
                tradeDuration = "4 days",
            )

        // When
        val result = dto.toDomain()

        // Then
        assertEquals("XMR", result.code)
        assertEquals("Monero", result.name)
        assertEquals("PRIVACY", result.category)
        assertEquals(true, result.supportAutoConf)
        assertEquals("5000.00", result.tradeLimitInfo)
        assertEquals("4 days", result.tradeDuration)
    }
}
