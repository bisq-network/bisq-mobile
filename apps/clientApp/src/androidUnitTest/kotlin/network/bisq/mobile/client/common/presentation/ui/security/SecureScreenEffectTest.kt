package network.bisq.mobile.client.common.presentation.ui.security

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.client.common.test_utils.TestApplication
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Verifies the Android [SecureScreenEffect] toggles `FLAG_SECURE` on the host window in step
 * with the composition lifecycle, so the pairing screen is protected only while it is shown.
 */
@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
class SecureScreenEffectTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun isWindowSecure(): Boolean {
        val flags = composeTestRule.activity.window.attributes.flags
        return flags and WindowManager.LayoutParams.FLAG_SECURE != 0
    }

    @Test
    fun `when secure screen is shown then window is flagged secure`() {
        composeTestRule.setContent { SecureScreenEffect() }
        composeTestRule.waitForIdle()

        assertTrue("FLAG_SECURE should be set while the secure screen is shown", isWindowSecure())
    }

    @Test
    fun `when secure screen leaves composition then secure flag is cleared`() {
        var showSecureScreen by mutableStateOf(true)
        composeTestRule.setContent {
            if (showSecureScreen) {
                SecureScreenEffect()
            }
        }
        composeTestRule.waitForIdle()
        assertTrue("precondition: window secure while shown", isWindowSecure())

        showSecureScreen = false
        composeTestRule.waitForIdle()

        assertFalse("FLAG_SECURE should be cleared once the secure screen is gone", isWindowSecure())
    }

    @Test
    fun `when two concurrent effects share a window then flag stays until the last one leaves`() {
        var showFirst by mutableStateOf(true)
        var showSecond by mutableStateOf(true)
        composeTestRule.setContent {
            if (showFirst) SecureScreenEffect()
            if (showSecond) SecureScreenEffect()
        }
        composeTestRule.waitForIdle()
        assertTrue("precondition: window secure while both shown", isWindowSecure())

        // First owner leaves — the second still needs the flag.
        showFirst = false
        composeTestRule.waitForIdle()
        assertTrue("FLAG_SECURE must remain while another owner is active", isWindowSecure())

        // Last owner leaves — now it may be cleared.
        showSecond = false
        composeTestRule.waitForIdle()
        assertFalse("FLAG_SECURE should be cleared only after the last owner leaves", isWindowSecure())
    }

    @Test
    fun `when window is already secured by host then effect preserves the flag on dispose`() {
        // Host secures the window before any secure screen is shown.
        composeTestRule.activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        var showSecureScreen by mutableStateOf(true)
        composeTestRule.setContent {
            if (showSecureScreen) {
                SecureScreenEffect()
            }
        }
        composeTestRule.waitForIdle()
        assertTrue("precondition: window secure", isWindowSecure())

        showSecureScreen = false
        composeTestRule.waitForIdle()

        assertTrue("host-owned FLAG_SECURE must survive the effect's lifecycle", isWindowSecure())
    }
}
