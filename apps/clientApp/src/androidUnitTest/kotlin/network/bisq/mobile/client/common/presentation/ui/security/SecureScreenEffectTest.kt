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
}
