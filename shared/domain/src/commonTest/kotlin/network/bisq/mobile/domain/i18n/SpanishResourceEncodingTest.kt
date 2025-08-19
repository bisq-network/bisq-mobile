package network.bisq.mobile.domain.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit test to verify that Spanish resource bundles contain properly rendered Spanish characters
 * and that the source properties files use Unicode escape sequences for Java properties compatibility.
 */
class SpanishResourceEncodingTest {

    /**
     * Common mojibake patterns that indicate encoding issues
     */
    private val mojibakePatterns = listOf(
        "Ã¡", "Ã©", "Ã­", "Ã³", "Ãº", "Ã±", "Ã¼", // á, é, í, ó, ú, ñ, ü encoded as ISO-8859-1
        "Â¿", "Â¡", // ¿, ¡ encoded as ISO-8859-1
        "ÃÂ", // Double-encoded patterns
        "Ã", "Â" // General mojibake markers
    )

    @Test
    fun `generated Spanish resource bundle should contain proper Spanish characters`() {
        // Import the generated Spanish resource bundle
        val spanishBundle: Map<String,String> = network.bisq.mobile.i18n.GeneratedResourceBundles_es.bundles["mobile"]!!

        // Verify the bundle contains proper Spanish characters (not escape sequences or mojibake)
        val expectedSpanishChars = listOf("ó", "á", "é", "í", "ú", "ñ", "¿", "¡")

        // Check a few key entries that should contain Spanish characters
        val testEntries = listOf(
            spanishBundle["error.exception"] ?: "",
            spanishBundle["confirmation.areYouSure"] ?: "",
            spanishBundle["min"] ?: "",
            spanishBundle["max"] ?: "",
            spanishBundle["mobile.components.marketFilter.sortBy.mostOffers"] ?: "",
            spanishBundle["mobile.validations.amountValidator.invalidNumber"] ?: ""
        )

        // Verify at least some entries contain Spanish characters
        val hasSpanishChars = testEntries.any { entry ->
            expectedSpanishChars.any { char -> entry.contains(char) }
        }

        assertTrue(
            hasSpanishChars,
            "Generated Spanish bundle should contain Spanish characters like ó, á, é, í, ú, ñ, ¿, ¡"
        )

        // Verify no mojibake patterns in any of the test entries
        testEntries.forEach { entry ->
            mojibakePatterns.forEach { pattern ->
                assertFalse(
                    entry.contains(pattern),
                    "Generated bundle entry '$entry' should not contain mojibake pattern: $pattern"
                )
            }
        }
    }

    @Test
    fun `Spanish bundle should contain expected translations`() {
        val spanishBundle: Map<String,String> = network.bisq.mobile.i18n.GeneratedResourceBundles_es.bundles["mobile"]!!

        // Test specific key translations that should be in Spanish
        val expectedTranslations = mapOf(
            "error.exception" to "Excepción",
            "confirmation.areYouSure" to "¿Estás seguro?",
            "min" to "Mín",
            "max" to "Máx"
        )

        expectedTranslations.forEach { (key, expectedSpanish) ->
            val actualValue = spanishBundle[key]
            assertEquals(actualValue, expectedSpanish, "Key '$key' should be '$expectedSpanish' but was '$actualValue'")
        }
    }

    @Test
    fun `Spanish bundle should not contain mojibake patterns`() {
        val spanishBundle: Map<String,String> = network.bisq.mobile.i18n.GeneratedResourceBundles_es.bundles["default"]!!

        // Get all values from the bundle
        val allValues = spanishBundle.values

        // Check that no values contain mojibake patterns
        allValues.forEach { value ->
            mojibakePatterns.forEach { pattern ->
                assertFalse(
                    value.contains(pattern),
                    "Spanish bundle value '$value' contains mojibake pattern '$pattern'. " +
                    "This indicates encoding corruption in the source properties file."
                )
            }
        }
    }

    @Test
    fun `Spanish bundle should contain accented characters`() {
        val spanishBundle: Map<String,String> = network.bisq.mobile.i18n.GeneratedResourceBundles_es.bundles["default"]!!

        // Get all values from the bundle
        val allValues = spanishBundle.values
        val allText = allValues.joinToString(" ")

        // Verify that the bundle contains Spanish accented characters
        // This ensures the Unicode escape sequences in the source were properly converted
        val spanishChars = listOf('á', 'é', 'í', 'ó', 'ú', 'ñ', '¿', '¡')
        val foundChars = spanishChars.filter { char -> allText.contains(char) }

        assertTrue(
            foundChars.isNotEmpty(),
            "Spanish bundle should contain at least some Spanish accented characters. " +
            "Found: ${foundChars.joinToString(", ")}. " +
            "This indicates the Unicode escape sequences in the source properties are working correctly."
        )
    }
}
