package network.bisq.mobile.domain.utils
import network.bisq.mobile.domain.utils.StringUtils.urlEncode
import kotlin.test.Test
import kotlin.test.assertEquals

class StringUtilsTest {

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
    fun `handles surrogate pairs (emoji) correctly`() {
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
