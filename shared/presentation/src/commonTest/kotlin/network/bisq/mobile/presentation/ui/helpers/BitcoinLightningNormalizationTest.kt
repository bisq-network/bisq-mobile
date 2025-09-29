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

    // Additional cleanForValidation tests (inside class for Android JUnit4 runner)
    @Test
    fun clean_bitcoin_base58_with_query() {
        val input = "bitcoin:1Base58Addr?amount=1.23"
        val cleaned = BitcoinLightningNormalization.cleanForValidation(input)
        assertEquals("1Base58Addr", cleaned)
    }

    @Test
    fun clean_bitcoin_with_double_slash_and_fragment() {
        val input = "bitcoin://bc1qxyz123#note"
        val cleaned = BitcoinLightningNormalization.cleanForValidation(input)
        assertEquals("bc1qxyz123", cleaned)
    }

    @Test
    fun clean_bitcoin_uppercase_scheme_and_lowercase_bech32() {
        val input = "BITCOIN:bc1QAbC"
        val cleaned = BitcoinLightningNormalization.cleanForValidation(input)
        assertEquals("bc1qabc", cleaned)
    }

    @Test
    fun clean_lightning_prefix_raw_invoice() {
        val input = "lightning:lnbc1ABCDEF"
        val cleaned = BitcoinLightningNormalization.cleanForValidation(input)
        assertEquals("lnbc1abcdef", cleaned)
    }

    @Test
    fun clean_lightning_with_double_slash_invoice() {
        val input = "lightning://lnbc1DeF"
        val cleaned = BitcoinLightningNormalization.cleanForValidation(input)
        assertEquals("lnbc1def", cleaned)
    }

    @Test
    fun clean_raw_bech32_lowercased() {
        val input = "bc1qAbCd"
        val cleaned = BitcoinLightningNormalization.cleanForValidation(input)
        assertEquals("bc1qabcd", cleaned)
    }

    @Test
    fun clean_preserves_raw_base58() {
        val input = "1AbcDEF"
        val cleaned = BitcoinLightningNormalization.cleanForValidation(input)
        assertEquals("1AbcDEF", cleaned)
    }

    @Test
    fun clean_raw_lightning_invoice_lowercased() {
        val input = "lnbc1AbCdE"
        val cleaned = BitcoinLightningNormalization.cleanForValidation(input)
        assertEquals("lnbc1abcde", cleaned)
    }

}

