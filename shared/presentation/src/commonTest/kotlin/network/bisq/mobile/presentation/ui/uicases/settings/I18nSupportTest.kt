package network.bisq.mobile.presentation.ui.uicases.settings

import kotlinx.coroutines.test.runTest
import network.bisq.mobile.i18n.I18nSupport
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class I18nSupportTest {

    @BeforeTest
    fun setupI18n() {
        I18nSupport.initialize("en")
    }

    @Test
    fun `getLocalizedNA should return localized N_A value`() = runTest {
        // Test that the localized N/A value is returned correctly
        val localizedNA = UserProfilePresenter.getLocalizedNA()
        assertEquals("N/A", localizedNA)
    }

    @Test
    fun `getLocalizedNA should return different values for different languages`() = runTest {
        // Test English
        I18nSupport.initialize("en")
        val englishNA = UserProfilePresenter.getLocalizedNA()
        assertEquals("N/A", englishNA)

        // Test German
        I18nSupport.initialize("de")
        val germanNA = UserProfilePresenter.getLocalizedNA()
        assertEquals("N/A", germanNA) // German translation for N/A

        // Test Russian
        I18nSupport.initialize("ru")
        val russianNA = UserProfilePresenter.getLocalizedNA()
        assertEquals("Недоступно", russianNA) // Russian translation for N/A
    }
}
