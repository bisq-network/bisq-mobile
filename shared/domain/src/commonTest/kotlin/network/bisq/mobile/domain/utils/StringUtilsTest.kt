package network.bisq.mobile.domain.utils

import network.bisq.mobile.domain.utils.StringUtils.randomAlphaNum
import network.bisq.mobile.domain.utils.StringUtils.truncate
import network.bisq.mobile.domain.utils.StringUtils.truncateBitcoinIdentifier
import network.bisq.mobile.domain.utils.StringUtils.urlEncode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringUtilsTest {
    /** truncate tests **/
    @Test
    fun `truncate returns original string when shorter than maxLength`() {
        assertEquals("hello", "hello".truncate(10))
    }

    @Test
    fun `truncate returns original string when equal to maxLength`() {
        assertEquals("hello", "hello".truncate(5))
    }

    @Test
    fun `truncate shortens string and adds ellipsis`() {
        assertEquals("hel...", "hello world".truncate(6))
    }

    @Test
    fun `truncate uses custom ellipsis`() {
        assertEquals("hello..", "hello world".truncate(7, ".."))
    }

    /** truncateBitcoinIdentifier tests **/
    @Test
    fun `truncateBitcoinIdentifier returns short strings as-is`() {
        assertEquals("", "".truncateBitcoinIdentifier())
        assertEquals("abc", "abc".truncateBitcoinIdentifier())
        assertEquals("abcdefghij", "abcdefghij".truncateBitcoinIdentifier())
        assertEquals("1234567890123456", "1234567890123456".truncateBitcoinIdentifier())
    }

    @Test
    fun `truncateBitcoinIdentifier takes first 8 and last 8 with three dots`() {
        // 17 chars: first8 + "..." + last8
        assertEquals("12345678...01234567", "12345678901234567".truncateBitcoinIdentifier())
        assertEquals("12345678...ijklmnop", "12345678abcdefghijklmnop".truncateBitcoinIdentifier())
    }

    /** randomAlphaNum tests **/
    @Test
    fun `randomAlphaNum generates string of correct length`() {
        assertEquals(20, randomAlphaNum().length)
        assertEquals(10, randomAlphaNum(10).length)
        assertEquals(1, randomAlphaNum(1).length)
    }

    @Test
    fun `randomAlphaNum contains only alphanumeric characters`() {
        val result = randomAlphaNum(100)
        assertTrue(result.all { it.isLetterOrDigit() })
    }

    /** url-encoding start **/
    @Test
    fun `encodes simple ASCII safely`() {
        assertEquals("hello", "hello".urlEncode())
        assertEquals("abc-_.~", "abc-_.~".urlEncode()) // allowed characters
    }

    @Test
    fun `encodes spaces and special characters`() {
        assertEquals("hello%20world", "hello world".urlEncode())
        assertEquals("%23hash", "#hash".urlEncode()) // '#' must be encoded
        assertEquals("a%2Bb", "a+b".urlEncode()) // '+' must be encoded under RFC 3986
    }

    @Test
    fun `encodes multibyte unicode correctly`() {
        assertEquals("%C3%A9", "é".urlEncode()) // U+00E9 LATIN SMALL LETTER E WITH ACUTE
        assertEquals("%E2%9C%94", "✔".urlEncode()) // U+2714 CHECK MARK
    }

    @Test
    fun `handles surrogate pairs - emoji - correctly`() {
        assertEquals("%F0%9F%98%80", "😀".urlEncode()) // U+1F600 GRINNING FACE
        assertEquals("abc%F0%9F%98%80xyz", "abc😀xyz".urlEncode())
    }

    @Test
    fun `preserves already percent-encoded sequences`() {
        assertEquals("%20", "%20".urlEncode()) // should not become %2520
        assertEquals("abc%20xyz", "abc%20xyz".urlEncode())
    }

    @Test
    fun `encodes stray percent sign`() {
        assertEquals("%25oops", "%oops".urlEncode()) // not followed by hex -> encode
    }

    @Test
    fun `encodes non-ASCII edge cases`() {
        assertEquals("%E6%97%A5%E6%9C%AC", "日本".urlEncode()) // Japanese word for Japan
        assertEquals("%F0%9F%8D%95%F0%9F%8D%94", "🍕🍔".urlEncode()) // pizza + burger emoji
    }
    /** url-encoding end **/
}
