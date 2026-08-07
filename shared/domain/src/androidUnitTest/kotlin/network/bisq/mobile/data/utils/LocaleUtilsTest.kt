package network.bisq.mobile.data.utils

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocaleUtilsTest {
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `locale builds language and region`() {
        val result = locale("pt", "BR")

        assertEquals("pt", result.language)
        assertEquals("BR", result.country)
    }

    @Test
    fun `setDefaultLocale applies well formed language tag`() {
        setDefaultLocale("de")

        assertEquals("de", Locale.getDefault().language)
    }

    @Test
    fun `setDefaultLocale falls back to English for ill formed tags`() {
        setDefaultLocale("!!!")

        assertEquals(Locale.ENGLISH.language, Locale.getDefault().language)
        assertEquals(Locale.ENGLISH.country, Locale.getDefault().country)
    }

    @Test
    fun `setDefaultLocale falls back to English for partially parsed tags`() {
        // forLanguageTag("en-X") would silently become "en"; Builder rejects it.
        setDefaultLocale("en-X")

        assertEquals(Locale.ENGLISH.language, Locale.getDefault().language)
        assertEquals(Locale.ENGLISH.country, Locale.getDefault().country)
    }

    @Test
    fun `setDefaultLocale applies hyphenated BCP47 tags`() {
        setDefaultLocale("pt-BR")

        assertEquals("pt", Locale.getDefault().language)
        assertEquals("BR", Locale.getDefault().country)
    }

    @Test
    fun `isComposePreviewLocaleSandbox is true for RenderSecurityException name`() {
        assertTrue(isComposePreviewLocaleSandbox(RenderSecurityExceptionStub("user.language")))
    }

    @Test
    fun `isComposePreviewLocaleSandbox is false for ordinary SecurityException`() {
        assertFalse(isComposePreviewLocaleSandbox(SecurityException("denied")))
    }

    /** Stand-in whose binary name contains "RenderSecurity", matching AS preview failures. */
    private class RenderSecurityExceptionStub(
        message: String,
    ) : SecurityException(message)
}
