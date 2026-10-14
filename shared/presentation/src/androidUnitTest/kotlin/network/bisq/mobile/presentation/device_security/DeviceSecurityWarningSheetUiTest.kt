package network.bisq.mobile.presentation.device_security

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.domain.model.PlatformType
import network.bisq.mobile.domain.service.device_security.DeviceSecurityWording
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceSecurityWarningSheetUiTest : BisqComposeUiTestBase() {
    private fun copyFor(
        app: DeviceSecurityAppVariant = DeviceSecurityAppVariant.CONNECT,
        platform: PlatformType = PlatformType.ANDROID,
        wording: DeviceSecurityWording = DeviceSecurityWording.FIRST_DETECTION,
        origin: DeviceSecurityOrigin = DeviceSecurityOrigin.DASHBOARD,
        isSuppressed: Boolean = false,
    ) = deviceSecurityCopy(
        DeviceSecurityUiState(
            app = app,
            platform = platform,
            wording = wording,
            origin = origin,
            isSuppressed = isSuppressed,
        ),
    )

    @Test
    fun `node copy names identity keys and says rooted`() {
        val copy = copyFor(app = DeviceSecurityAppVariant.NODE)

        assertEquals("mobile.deviceSecurity.title.rooted".i18n(), copy.title)
        assertEquals("mobile.deviceSecurity.body.node.rooted".i18n(), copy.body)
    }

    @Test
    fun `connect on android says rooted`() {
        val copy = copyFor()

        assertEquals("mobile.deviceSecurity.title.rooted".i18n(), copy.title)
        assertEquals("mobile.deviceSecurity.body.connect.rooted".i18n(), copy.body)
    }

    @Test
    fun `connect on ios says jailbroken`() {
        val copy = copyFor(platform = PlatformType.IOS)

        assertEquals("mobile.deviceSecurity.title.jailbroken".i18n(), copy.title)
        assertEquals("mobile.deviceSecurity.body.connect.jailbroken".i18n(), copy.body)
    }

    @Test
    fun `the changed wording adds the lead paragraph and keeps the body`() {
        val copy = copyFor(wording = DeviceSecurityWording.CHANGED_SINCE_SETUP)

        assertEquals("mobile.deviceSecurity.changedLead".i18n(), copy.changedLead)
        assertEquals("mobile.deviceSecurity.body.connect.rooted".i18n(), copy.body)
    }

    @Test
    fun `the first detection has no lead paragraph`() {
        assertNull(copyFor().changedLead)
    }

    @Test
    fun `from settings after the decision the note replaces the dont show again button`() {
        val copy = copyFor(origin = DeviceSecurityOrigin.SETTINGS, isSuppressed = true)

        assertNull(copy.dontShowAgain)
        assertEquals("mobile.deviceSecurity.suppressedNote".i18n(), copy.suppressedNote)
    }

    @Test
    fun `on the dashboard the dont show again button is offered`() {
        val copy = copyFor()

        assertEquals("mobile.deviceSecurity.dontShowAgain".i18n(), copy.dontShowAgain)
        assertNull(copy.suppressedNote)
    }

    @Test
    fun `the copy has no missing translations`() {
        listOf(
            copyFor(app = DeviceSecurityAppVariant.NODE, wording = DeviceSecurityWording.CHANGED_SINCE_SETUP),
            copyFor(platform = PlatformType.IOS, origin = DeviceSecurityOrigin.SETTINGS, isSuppressed = true),
        ).forEach { copy ->
            listOfNotNull(copy.title, copy.changedLead, copy.body, copy.gotIt, copy.dontShowAgain, copy.suppressedNote)
                .forEach { text -> assert(!text.startsWith("mobile.deviceSecurity")) { "untranslated: $text" } }
        }
    }

    @Test
    fun `got it and dont show again report their actions`() {
        var gotIt = 0
        var dontShowAgain = 0
        setTestContent {
            DeviceSecurityWarningContent(
                copy = copyFor(),
                onGotIt = { gotIt++ },
                onDontShowAgain = { dontShowAgain++ },
            )
        }

        composeTestRule.onNodeWithTag("device_security_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("device_security_got_it").performClick()
        composeTestRule.onNodeWithTag("device_security_dont_show_again").performClick()

        assertEquals(1, gotIt)
        assertEquals(1, dontShowAgain)
    }

    @Test
    fun `the settings row opens the sheet`() {
        val actions = mutableListOf<DeviceSecurityUiAction>()
        setTestContent {
            DeviceSecuritySettingsRow(
                uiState = DeviceSecurityUiState(isRowVisible = true),
                onAction = { actions += it },
            )
        }

        composeTestRule.onNodeWithText("mobile.deviceSecurity.settings.label".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithTag("device_security_settings_row").performClick()

        assertEquals(listOf<DeviceSecurityUiAction>(DeviceSecurityUiAction.OnOpenFromSettings), actions)
    }

    @Test
    fun `no settings row while the device is not detected as rooted`() {
        setTestContent {
            DeviceSecuritySettingsRow(
                uiState = DeviceSecurityUiState(isRowVisible = false),
                onAction = {},
            )
        }

        composeTestRule.onNodeWithTag("device_security_settings_row").assertDoesNotExist()
    }
}
