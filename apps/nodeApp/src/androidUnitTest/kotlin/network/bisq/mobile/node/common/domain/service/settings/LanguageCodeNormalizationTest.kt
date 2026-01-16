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
                "en" to Locale("en", "US"),
                "de" to Locale("de", "DE"),
                "fr" to Locale("fr", "FR"),
                "es" to Locale("es", "ES"),
                "it" to Locale("it", "IT"),
                "ru" to Locale("ru", "RU"),
                "cs" to Locale("cs", "CZ"),
                "hi" to Locale("hi", "IN"),
                "id" to Locale("id", "ID"),
                "tr" to Locale("tr", "TR"),
                "vi" to Locale("vi", "VN"),
                "pt-BR" to Locale("pt", "BR"),
                "af-ZA" to Locale("af", "ZA"),
                "pcm-NG" to Locale("pcm", "NG"),
            )

        testCases.forEach { (code, expectedLocale) ->
            val result = invokeLanguageCodeToLocale(code)
            assertEquals("Language code '$code' should map to correct locale", expectedLocale, result)
        }
    }

    @Test
    fun `languageCodeToLocale should normalize and convert legacy codes`() {
        // Legacy pcm -> pcm-NG -> Locale("pcm", "NG")
        assertEquals(Locale("pcm", "NG"), invokeLanguageCodeToLocale("pcm"))

        // Underscore variant pt_BR -> pt-BR -> Locale("pt", "BR")
        assertEquals(Locale("pt", "BR"), invokeLanguageCodeToLocale("pt_BR"))

        // Underscore variant af_ZA -> af-ZA -> Locale("af", "ZA")
        assertEquals(Locale("af", "ZA"), invokeLanguageCodeToLocale("af_ZA"))
    }

    @Test
    fun `languageCodeToLocale should fall back to en-US for unsupported codes`() {
        assertEquals(Locale("en", "US"), invokeLanguageCodeToLocale("xyz"))
        assertEquals(Locale("en", "US"), invokeLanguageCodeToLocale("invalid"))
        assertEquals(Locale("en", "US"), invokeLanguageCodeToLocale(""))
    }

    @Test
    fun `languageCodeToLocale should be idempotent for valid codes`() {
        val locale1 = invokeLanguageCodeToLocale("de")
        val locale2 = invokeLanguageCodeToLocale("de")
        assertEquals(locale1, locale2)
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
