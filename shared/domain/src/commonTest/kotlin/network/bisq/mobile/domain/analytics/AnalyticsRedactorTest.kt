package network.bisq.mobile.domain.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests the redactor's pattern coverage against the privacy agreement on
 * bisq-network/bisq-mobile#525.
 *
 * The redactor is a defense-in-depth layer; the sealed [AnalyticsEvent] API is
 * the primary guarantee that no PII reaches a payload. But because exception
 * messages and stack frames can carry arbitrary strings constructed at runtime
 * (file paths from the JVM, error messages quoting user input, etc.), every
 * outbound payload must pass through here.
 *
 * Each pattern has at least one positive ("is redacted") and one negative
 * ("isn't false-positively redacted") test. We assert on the REDACTED token
 * being present in the output, not on full string equality, because the
 * redactor may evolve to add/remove neighbouring context.
 */
class AnalyticsRedactorTest {
    private val redactor = AnalyticsRedactor()

    // ============ EMAIL ============

    @Test
    fun `redacts a bare email address`() {
        val input = "Failed to deliver to alice@example.com after 3 retries"
        val output = redactor.redact(input)
        assertFalse(output.contains("alice@example.com"))
        assertTrue(output.contains("<email>"))
    }

    @Test
    fun `redacts an email with subdomain and plus-tag`() {
        val input = "user bob+work@mail.bisq.network logged in"
        val output = redactor.redact(input)
        assertFalse(output.contains("bob+work@mail.bisq.network"))
    }

    @Test
    fun `does not redact plain at-mention without TLD`() {
        val input = "ping @rodvar in chat"
        val output = redactor.redact(input)
        assertEquals(input, output)
    }

    // ============ BITCOIN ADDRESS ============

    @Test
    fun `redacts a legacy P2PKH address`() {
        val input = "send to 1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2 now"
        val output = redactor.redact(input)
        assertFalse(output.contains("1BvBMSEYstWetqTFn5Au4m4GFg7xJaNVN2"))
        assertTrue(output.contains("<btc_address>"))
    }

    @Test
    fun `redacts a P2SH address`() {
        val input = "multisig 3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy held"
        val output = redactor.redact(input)
        assertFalse(output.contains("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"))
    }

    @Test
    fun `redacts a bech32 segwit address`() {
        val input = "destination bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4 confirmed"
        val output = redactor.redact(input)
        assertFalse(output.contains("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"))
    }

    @Test
    fun `redacts a bech32m taproot address`() {
        // bech32m starts with bc1p (P2TR)
        val input = "taproot bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0 here"
        val output = redactor.redact(input)
        assertFalse(output.contains("bc1p0xlxvlhemja6c4dqv22uapctqupfhlxm9h8z3k2e72q4k9hcz7vqzk5jj0"))
    }

    @Test
    fun `does not redact short hex-looking strings that aren't addresses`() {
        val input = "trade id abc123 went through"
        val output = redactor.redact(input)
        assertEquals(input, output)
    }

    // ============ ONION ADDRESS ============

    @Test
    fun `redacts a v3 onion hostname`() {
        // v3 onions are 56 chars of base32 + .onion
        val input = "connecting to uzlqr4bnoscfifvjvzj3kicoc23ejibefpsynuorcvetjptzqkaqisid.onion now"
        val output = redactor.redact(input)
        assertFalse(output.contains("uzlqr4bnoscfifvjvzj3kicoc23ejibefpsynuorcvetjptzqkaqisid.onion"))
        assertTrue(output.contains("<onion>"))
    }

    @Test
    fun `redacts an onion URL with scheme and path`() {
        val input = "GET http://uzlqr4bnoscfifvjvzj3kicoc23ejibefpsynuorcvetjptzqkaqisid.onion/api/0 -> 200"
        val output = redactor.redact(input)
        assertFalse(output.contains("uzlqr4bnoscfifvjvzj3kicoc23ejibefpsynuorcvetjptzqkaqisid.onion"))
    }

    // ============ SEED PHRASE ============

    @Test
    fun `redacts a 12-word seed-phrase shape`() {
        val input = "user pasted: abandon ability able about above absent absorb abstract absurd abuse access accident"
        val output = redactor.redact(input)
        assertFalse(output.contains("abandon ability"))
        assertTrue(output.contains("<seed_phrase>"))
    }

    @Test
    fun `redacts a 24-word seed-phrase shape`() {
        val input = (1..24).joinToString(" ") { "word" }
        val output = redactor.redact("MNEMONIC=$input REST")
        assertFalse(output.contains("word word word"))
    }

    @Test
    fun `does not redact short lowercase prose`() {
        val input = "this is just normal english text in the log"
        val output = redactor.redact(input)
        assertEquals(input, output)
    }

    // ============ FILE PATHS ============

    @Test
    fun `redacts a macOS user home path`() {
        val input = "FileNotFoundException: /Users/rodvar/.bisq/keystore"
        val output = redactor.redact(input)
        assertFalse(output.contains("/Users/rodvar"))
        assertTrue(output.contains("<path>"))
    }

    @Test
    fun `redacts a linux user home path`() {
        val input = "could not open /home/dev/.local/share/bisq.db"
        val output = redactor.redact(input)
        assertFalse(output.contains("/home/dev"))
    }

    @Test
    fun `redacts a windows user home path`() {
        val input = """opened C:\Users\Alice\AppData\Local\Bisq"""
        val output = redactor.redact(input)
        assertFalse(output.contains("""C:\Users\Alice"""))
    }

    @Test
    fun `does not redact system paths without username segment`() {
        val input = "couldn't read /etc/hosts"
        val output = redactor.redact(input)
        assertEquals(input, output)
    }

    // ============ IP ADDRESSES ============

    @Test
    fun `redacts an IPv4 address`() {
        val input = "connecting to 192.168.1.42 timed out"
        val output = redactor.redact(input)
        assertFalse(output.contains("192.168.1.42"))
        assertTrue(output.contains("<ip>"))
    }

    @Test
    fun `redacts an IPv6 address`() {
        val input = "peer at 2001:0db8:85a3:0000:0000:8a2e:0370:7334 disconnected"
        val output = redactor.redact(input)
        assertFalse(output.contains("2001:0db8:85a3"))
    }

    @Test
    fun `does not redact pure version-like dotted numbers`() {
        val input = "running version 0.4.1 on build 30"
        val output = redactor.redact(input)
        assertEquals(input, output)
    }

    // ============ COMPOSITION ============

    @Test
    fun `redacts multiple PII types in the same string`() {
        val input =
            "User alice@example.com from 10.0.0.1 sent to bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4 " +
                "via /Users/alice/.bisq/keys"
        val output = redactor.redact(input)
        assertFalse(output.contains("alice@example.com"))
        assertFalse(output.contains("10.0.0.1"))
        assertFalse(output.contains("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"))
        assertFalse(output.contains("/Users/alice"))
    }

    @Test
    fun `is null-safe on empty input`() {
        assertEquals("", redactor.redact(""))
    }

    @Test
    fun `is identity on strings with no PII`() {
        val input = "trade.completed in 4.2s"
        assertEquals(input, redactor.redact(input))
    }
}
