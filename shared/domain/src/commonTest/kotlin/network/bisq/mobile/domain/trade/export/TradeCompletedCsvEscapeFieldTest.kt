package network.bisq.mobile.domain.trade.export

import kotlin.test.Test
import kotlin.test.assertEquals

class TradeCompletedCsvEscapeFieldTest {
    @Test
    fun escapeCsvField_returnsUnchanged_whenNoSpecialCharacters() {
        assertEquals("plain", TradeCompletedCsv.escapeCsvField("plain"))
        assertEquals("a b 9", TradeCompletedCsv.escapeCsvField("a b 9"))
    }

    @Test
    fun escapeCsvField_wrapsInQuotes_whenContainsComma() {
        assertEquals("\"a,b\"", TradeCompletedCsv.escapeCsvField("a,b"))
    }

    @Test
    fun escapeCsvField_doublesQuotes_whenContainsQuote() {
        assertEquals("\"say \"\"hi\"\"\"", TradeCompletedCsv.escapeCsvField("say \"hi\""))
    }

    @Test
    fun escapeCsvField_wrapsInQuotes_whenContainsNewline() {
        assertEquals("\"line1\nline2\"", TradeCompletedCsv.escapeCsvField("line1\nline2"))
    }

    @Test
    fun escapeCsvField_wrapsInQuotes_whenContainsCarriageReturn() {
        assertEquals("\"a\rb\"", TradeCompletedCsv.escapeCsvField("a\rb"))
    }

    @Test
    fun escapeCsvField_wrapsInQuotes_whenContainsCrlf() {
        assertEquals("\"a\r\nb\"", TradeCompletedCsv.escapeCsvField("a\r\nb"))
    }

    @Test
    fun escapeCsvField_handlesCommaAndQuoteTogether() {
        assertEquals("\"a,\"\"b\"\",c\"", TradeCompletedCsv.escapeCsvField("a,\"b\",c"))
    }

    @Test
    fun escapeCsvField_emptyString() {
        assertEquals("", TradeCompletedCsv.escapeCsvField(""))
    }
}
