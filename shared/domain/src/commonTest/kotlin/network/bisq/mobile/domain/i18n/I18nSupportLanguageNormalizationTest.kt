package network.bisq.mobile.domain.i18n

import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for I18nSupport language code normalization and fallback behavior.
 * Verifies that currentLanguage is aligned with the actual bundles loaded.
 */
class I18nSupportLanguageNormalizationTest {
    @BeforeTest
    fun setup() {
        // Reset to English before each test
        I18nSupport.initialize("en")
    }

    @Test
    fun `setLanguage with valid code should set currentLanguage to that code`() {
        I18nSupport.setLanguage("de")
        assertEquals("de", I18nSupport.currentLanguage)

        I18nSupport.setLanguage("fr")
        assertEquals("fr", I18nSupport.currentLanguage)

        I18nSupport.setLanguage("pcm-NG")
        assertEquals("pcm-NG", I18nSupport.currentLanguage)
    }

    @Test
    fun `setLanguage with unsupported code should fall back to en`() {
        I18nSupport.setLanguage("xyz")
        assertEquals("en", I18nSupport.currentLanguage, "currentLanguage should be 'en' when falling back to English bundles")
    }

    @Test
    fun `setLanguage with blank code should fall back to en`() {
        I18nSupport.setLanguage("")
        assertEquals("en", I18nSupport.currentLanguage)
    }

    @Test
    fun `setLanguage with legacy pcm should fall back to en`() {
        // "pcm" is not in LANGUAGE_CODE_TO_BUNDLE_MAP, only "pcm-NG" is
        I18nSupport.setLanguage("pcm")
        assertEquals("en", I18nSupport.currentLanguage, "Legacy 'pcm' should fall back to 'en' since only 'pcm-NG' is supported")
    }

    @Test
    fun `setLanguage with underscore variant should fall back to en`() {
        // "pt_BR" is not in LANGUAGE_CODE_TO_BUNDLE_MAP, only "pt-BR" is
        I18nSupport.setLanguage("pt_BR")
        assertEquals("en", I18nSupport.currentLanguage, "Underscore variant 'pt_BR' should fall back to 'en' since only 'pt-BR' is supported")
    }

    @Test
    fun `setLanguage with valid hyphenated code should work`() {
        I18nSupport.setLanguage("pt-BR")
        assertEquals("pt-BR", I18nSupport.currentLanguage)

        I18nSupport.setLanguage("af-ZA")
        assertEquals("af-ZA", I18nSupport.currentLanguage)
    }

    @Test
    fun `currentLanguage should match loaded bundles`() {
        // When we set a valid language, currentLanguage should match
        I18nSupport.setLanguage("de")
        assertEquals("de", I18nSupport.currentLanguage)

        // When we set an invalid language, currentLanguage should be "en" (matching the fallback bundles)
        I18nSupport.setLanguage("invalid-code")
        assertEquals("en", I18nSupport.currentLanguage)
    }

    @Test
    fun `initialize with valid code should set currentLanguage correctly`() {
        I18nSupport.initialize("ru")
        assertEquals("ru", I18nSupport.currentLanguage)
        assertEquals(true, I18nSupport.isReady)
    }

    @Test
    fun `initialize with invalid code should fall back to en`() {
        I18nSupport.initialize("invalid")
        assertEquals("en", I18nSupport.currentLanguage)
        assertEquals(true, I18nSupport.isReady)
    }

    @Test
    fun `all supported language codes should work`() {
        val supportedCodes =
            listOf(
                "en",
                "af-ZA",
                "cs",
                "de",
                "es",
                "fr",
                "hi",
                "id",
                "it",
                "pcm-NG",
                "pt-BR",
                "ru",
                "tr",
                "vi",
            )

        supportedCodes.forEach { code ->
            I18nSupport.setLanguage(code)
            assertEquals(code, I18nSupport.currentLanguage, "Language code '$code' should be set correctly")
        }
    }

    @Test
    fun `switching between languages should update currentLanguage`() {
        I18nSupport.setLanguage("en")
        assertEquals("en", I18nSupport.currentLanguage)

        I18nSupport.setLanguage("de")
        assertEquals("de", I18nSupport.currentLanguage)

        I18nSupport.setLanguage("fr")
        assertEquals("fr", I18nSupport.currentLanguage)

        I18nSupport.setLanguage("en")
        assertEquals("en", I18nSupport.currentLanguage)
    }

    @Test
    fun `setLanguage should be idempotent`() {
        I18nSupport.setLanguage("de")
        assertEquals("de", I18nSupport.currentLanguage)

        I18nSupport.setLanguage("de")
        assertEquals("de", I18nSupport.currentLanguage)
    }
}
