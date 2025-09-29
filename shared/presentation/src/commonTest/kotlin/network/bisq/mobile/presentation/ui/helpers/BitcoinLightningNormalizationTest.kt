package network.bisq.mobile.presentation.ui.helpers

import kotlin.test.Test
import kotlin.test.assertEquals

class BitcoinLightningNormalizationTest {

    @Test
    fun uppercase_bech32_bitcoin_address_is_lowercased() {
        val input = "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KYGT080" // sample length
        val expected = input.lowercase()
        assertEquals(expected, BitcoinLightningNormalization.normalizeScan(input))
    }

    @Test
    fun base58_legacy_address_is_unchanged() {
        val input = "1BoatSLRHtKNngkdXEeobR76b53LETtpyT"
        assertEquals(input, BitcoinLightningNormalization.normalizeScan(input))
    }

    @Test
    fun lightning_invoice_raw_is_lowercased() {
        val input = "LNBC2500U1PSHJ9DPP5ZJ...XYZ" // shortened example; case should be normalized
        val expected = input.lowercase()
        assertEquals(expected, BitcoinLightningNormalization.normalizeScan(input))
    }

    @Test
    fun lightning_uri_is_lowercased_after_prefix() {
        val input = "LIGHTNING:LNBC10N1P...ABC"
        val out = BitcoinLightningNormalization.normalizeScan(input)
        // prefix preserved, body lowercased
        assertEquals("lightning:" + input.substring(10).lowercase(), out)
    }

    @Test
    fun bip21_with_bech32_only_address_is_lowercased_query_preserved() {
        val input = "bitcoin:BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KYGT080?amount=0.01&label=Wasabi%20Pay"
        val out = BitcoinLightningNormalization.normalizeScan(input)
        val expected = "bitcoin:" + "BC1QW508D6QEJXTDG4Y5R3ZARVARY0C5XW7KYGT080".lowercase() + "?amount=0.01&label=Wasabi%20Pay"
        assertEquals(expected, out)
    }

    @Test
    fun bip21_with_base58_address_is_unchanged() {
        val input = "bitcoin:1BoatSLRHtKNngkdXEeobR76b53LETtpyT?amount=1.23&label=TestLabel"
        assertEquals(input, BitcoinLightningNormalization.normalizeScan(input))
    }
}

