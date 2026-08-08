package network.bisq.mobile.node.common.domain.service.settings

import network.bisq.mobile.i18n.I18nSupport
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

/**
 * Tests for language code normalization logic in NodeSettingsServiceFacade.
 * Since the normalization methods are private in the companion object, we test
 * the behavior through the public languageCodeToLocale method via reflection.
 */
class LanguageCodeNormalizationTest {
    @Before
    fun setup() {
        // Initialize I18nSupport to ensure LANGUAGE_CODE_TO_BUNDLE_MAP is available
        I18nSupport.initialize("en")
    }

    @Test
    fun `normalizeLanguageCode should handle blank codes`() {
        val result = invokeNormalizeLanguageCode("")
        assertEquals("en", result)
    }

    @Test
    fun `normalizeLanguageCode should handle underscore variants`() {
        assertEquals("pt-BR", invokeNormalizeLanguageCode("pt_BR"))
        assertEquals("af-ZA", invokeNormalizeLanguageCode("af_ZA"))
    }

    @Test
    fun `normalizeLanguageCode should handle legacy pcm`() {
        val result = invokeNormalizeLanguageCode("pcm")
        assertEquals("pcm-NG", result)
    }

    @Test
    fun `normalizeLanguageCode should handle legacy en_US`() {
        val result = invokeNormalizeLanguageCode("en_US")
        assertEquals("en", result)
    }

    @Test
    fun `normalizeLanguageCode should handle valid codes`() {
        assertEquals("de", invokeNormalizeLanguageCode("de"))
        assertEquals("fr", invokeNormalizeLanguageCode("fr"))
        assertEquals("pcm-NG", invokeNormalizeLanguageCode("pcm-NG"))
        assertEquals("pt-BR", invokeNormalizeLanguageCode("pt-BR"))
    }

    @Test
    fun `normalizeLanguageCode should fall back to en for unsupported codes`() {
        assertEquals("en", invokeNormalizeLanguageCode("xyz"))
        assertEquals("en", invokeNormalizeLanguageCode("invalid"))
        assertEquals("en", invokeNormalizeLanguageCode("zz-ZZ"))
    }

    @Test
    fun `languageCodeToLocale should handle all supported languages`() {
        val testCases =
            mapOf(
                "en" to ("en" to "US"),
                "de" to ("de" to "DE"),
                "fr" to ("fr" to "FR"),
                "es" to ("es" to "ES"),
                "it" to ("it" to "IT"),
                "ru" to ("ru" to "RU"),
                "cs" to ("cs" to "CZ"),
                "hi" to ("hi" to "IN"),
                "id" to ("id" to "ID"),
                "tr" to ("tr" to "TR"),
                "vi" to ("vi" to "VN"),
                "pt-BR" to ("pt" to "BR"),
                "af-ZA" to ("af" to "ZA"),
                "pcm-NG" to ("pcm" to "NG"),
            )

        testCases.forEach { (code, expected) ->
            val (expectedLanguage, expectedCountry) = expected
            assertLocaleFields(code, expectedLanguage, expectedCountry)
        }
    }

    @Test
    fun `languageCodeToLocale should normalize and convert legacy codes`() {
        // Legacy pcm -> pcm-NG
        assertLocaleFields("pcm", "pcm", "NG")

        // Underscore variant pt_BR -> pt-BR
        assertLocaleFields("pt_BR", "pt", "BR")

        // Underscore variant af_ZA -> af-ZA
        assertLocaleFields("af_ZA", "af", "ZA")
    }

    @Test
    fun `languageCodeToLocale should fall back to en-US for unsupported codes`() {
        assertLocaleFields("xyz", "en", "US")
        assertLocaleFields("invalid", "en", "US")
        assertLocaleFields("", "en", "US")
    }

    @Test
    fun `languageCodeToLocale should be idempotent for valid codes`() {
        val locale1 = invokeLanguageCodeToLocale("de")
        val locale2 = invokeLanguageCodeToLocale("de")
        assertEquals(locale1, locale2)
    }

    private fun assertLocaleFields(
        languageCode: String,
        expectedLanguage: String,
        expectedCountry: String,
    ) {
        val result = invokeLanguageCodeToLocale(languageCode)
        assertEquals(
            "Language code '$languageCode' should map to language '$expectedLanguage'",
            expectedLanguage,
            result.language,
        )
        assertEquals(
            "Language code '$languageCode' should map to country '$expectedCountry'",
            expectedCountry,
            result.country,
        )
    }

    // Helper methods to invoke private companion object methods via reflection
    private fun invokeNormalizeLanguageCode(languageCode: String): String {
        val companionClass =
            NodeSettingsServiceFacade::class.java.declaredClasses
                .first { it.simpleName == "Companion" }
        val method = companionClass.getDeclaredMethod("normalizeLanguageCode", String::class.java)
        method.isAccessible = true
        val companion = NodeSettingsServiceFacade::class.java.getDeclaredField("Companion").get(null)
        return method.invoke(companion, languageCode) as String
    }

    private fun invokeLanguageCodeToLocale(languageCode: String): Locale {
        val companionClass =
            NodeSettingsServiceFacade::class.java.declaredClasses
                .first { it.simpleName == "Companion" }
        val method = companionClass.getDeclaredMethod("languageCodeToLocale", String::class.java)
        method.isAccessible = true
        val companion = NodeSettingsServiceFacade::class.java.getDeclaredField("Companion").get(null)
        return method.invoke(companion, languageCode) as Locale
    }
}
