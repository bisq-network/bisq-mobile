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

    @Test
    fun escapeCsvField_prefixesApostrophe_whenLeadingSpreadsheetFormulaChar_andNoOtherQuoting() {
        assertEquals("'=1+1", TradeCompletedCsv.escapeCsvField("=1+1"))
        assertEquals("'+2", TradeCompletedCsv.escapeCsvField("+2"))
        assertEquals("'-1", TradeCompletedCsv.escapeCsvField("-1"))
        assertEquals("'@ref", TradeCompletedCsv.escapeCsvField("@ref"))
    }

    @Test
    fun escapeCsvField_prefixesApostrophe_beforeQuotingWhenCommaOrQuotesPresent() {
        assertEquals("\"'=a,b\"", TradeCompletedCsv.escapeCsvField("=a,b"))
        assertEquals("\"'=\"\"a\"", TradeCompletedCsv.escapeCsvField("=\"a"))
    }

    @Test
    fun escapeCsvField_prefixesApostrophe_whenFirstCharIsTab() {
        assertEquals("'\tcell", TradeCompletedCsv.escapeCsvField("\tcell"))
    }

    @Test
    fun escapeCsvField_prefixesApostrophe_whenValueStartsWithCarriageReturn() {
        assertEquals("\"'\r\n\"", TradeCompletedCsv.escapeCsvField("\r\n"))
    }
}
