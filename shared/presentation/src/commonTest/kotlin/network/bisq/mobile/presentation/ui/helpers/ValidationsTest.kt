package network.bisq.mobile.presentation.ui.helpers

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ValidationsTest {

    @Test
    fun testLightningInvoiceValidation() {
        // Valid Lightning invoices - must start with lnbc/LNBC and follow the pattern
        assertTrue(LightningInvoiceValidation.validateInvoice("lnbc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq"))
        assertTrue(LightningInvoiceValidation.validateInvoice("LNBC1PVJLUEZPP5QQQSYQCYQ5RQWZQFQQQSYQCYQ5RQWZQFQQQSYQCYQ5RQWZQFQYPQDPL2PKX2CTNV5SXXMMWWD5KGETJYPEH2URSDAE8G6TWVUS8G6RFWVS8QUN0DFJKXAQ"))
        
        // Invalid Lightning invoices
        assertFalse(LightningInvoiceValidation.validateInvoice("lnbc")) // Too short
        assertFalse(LightningInvoiceValidation.validateInvoice("lnbc1abc")) // Too short
        assertFalse(LightningInvoiceValidation.validateInvoice("btc1pvjluezpp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdpl2pkx2ctnv5sxxmmwwd5kgetjypeh2ursdae8g6twvus8g6rfwvs8qun0dfjkxaq")) // Wrong prefix
        assertFalse(LightningInvoiceValidation.validateInvoice("")) // Empty
    }

    @Test
    fun testBitcoinAddressValidation() {
        // Valid Bitcoin addresses
        assertTrue(BitcoinAddressValidation.validateAddress("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")) // P2PKH
        assertTrue(BitcoinAddressValidation.validateAddress("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy")) // P2SH
        assertTrue(BitcoinAddressValidation.validateAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")) // Bech32 (SegWit)
        
        // Taproot addresses (Bech32m format starting with bc1p)
        assertTrue(BitcoinAddressValidation.validateAddress("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0")) // Valid Taproot
        assertTrue(BitcoinAddressValidation.validateAddress("bc1pqyqszqgpqyqszqgpqyqszqgpqyqszqgpqyqszqgpqyqszqgpqyqsyjer9e")) // Valid Taproot
        
        // Invalid Bitcoin addresses
        assertFalse(BitcoinAddressValidation.validateAddress("1A1zP1")) // Too short
        assertFalse(BitcoinAddressValidation.validateAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")) // Too long
        assertFalse(BitcoinAddressValidation.validateAddress("xc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4")) // Invalid prefix
        assertFalse(BitcoinAddressValidation.validateAddress("")) // Empty
        
        // Invalid Taproot addresses
        assertFalse(BitcoinAddressValidation.validateAddress("bc1p")) // Too short
        assertFalse(BitcoinAddressValidation.validateAddress("tc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0")) // Wrong network prefix
        assertFalse(BitcoinAddressValidation.validateAddress("bc2p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0")) // Wrong version prefix
    }

    @Test
    fun testLightningPreImageValidation() {
        // Valid preimages (64 hex characters)
        assertTrue(LightningPreImageValidation.validatePreImage("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c4e5f6a1b2c3d4e55f6a1b2c3d4e5f6a1b23"))
        assertTrue(LightningPreImageValidation.validatePreImage("0123456789abcdef01234589abcdef0123456789abcdef0123456789abcdef12"))
        
        // Invalid preimages
        assertFalse(LightningPreImageValidation.validatePreImage("a1b2c3")) // Too short
        assertFalse(LightningPreImageValidation.validatePreImage("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3")) // Too long
        assertFalse(LightningPreImageValidation.validatePreImage("g1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2")) // Invalid hex character
        assertFalse(LightningPreImageValidation.validatePreImage("")) // Empty
    }

    @Test
    fun testBitcoinTransactionValidation() {
        // Valid transaction IDs (64 hex characters)
        assertTrue(BitcoinTransactionValidation.validateTxId("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"))
        assertTrue(BitcoinTransactionValidation.validateTxId("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"))
        
        // Invalid transaction IDs
        assertFalse(BitcoinTransactionValidation.validateTxId("a1b2c3")) // Too short
        assertFalse(BitcoinTransactionValidation.validateTxId("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3")) // Too long
        assertFalse(BitcoinTransactionValidation.validateTxId("g1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2")) // Invalid hex character
        assertFalse(BitcoinTransactionValidation.validateTxId("")) // Empty
    }

    @Test
    fun testAmountValidator() {
        // For these tests, we need to mock the toDoubleOrNullLocaleAware function
        // Let's test with direct numeric values that should work
        
        // Valid amounts - assuming the conversion works correctly
        assertNull(AmountValidator.validate("10", 5 * 10000, 100 * 10000)) // 10 is between 5 and 100
        assertNull(AmountValidator.validate("5", 5 * 10000, 100 * 10000)) // Equal to min
        assertNull(AmountValidator.validate("100", 5 * 10000, 100 * 10000)) // Equal to max
        
        // Invalid amounts - assuming the conversion works correctly
        assertEquals("Should be greater than 5.0", AmountValidator.validate("4", 5 * 10000, 100 * 10000))
        assertEquals("Should be less than 100.0", AmountValidator.validate("101", 5 * 10000, 100 * 10000))
        
        // The "Invalid number" case is harder to test without mocking the extension function
    }
}