package network.bisq.mobile.data.utils

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AndroidAppUpdateLinkerTest {
    private companion object {
        private const val GOOGLE_PLAY_INSTALLER = "com.android.vending"
        private const val GOOGLE_PLAY_FEEDBACK = "com.google.android.feedback"
    }

    @Test
    fun `returns Play Store market deep link when installed from Google Play`() {
        val context = RuntimeEnvironment.getApplication()
        val linker =
            AndroidAppUpdateLinker(context) { GOOGLE_PLAY_INSTALLER }

        assertEquals(
            AppUpdateUrls.playStoreMarketUrl(context.packageName),
            linker.getUpdateUrl(),
        )
    }

    @Test
    fun `returns Play Store market deep link when installed from Google Play feedback installer`() {
        val context = RuntimeEnvironment.getApplication()
        val linker =
            AndroidAppUpdateLinker(context) { GOOGLE_PLAY_FEEDBACK }

        assertEquals(
            AppUpdateUrls.playStoreMarketUrl(context.packageName),
            linker.getUpdateUrl(),
        )
    }

    @Test
    fun `returns GitHub releases when installer is unknown`() {
        val context = RuntimeEnvironment.getApplication()
        val linker = AndroidAppUpdateLinker(context) { null }

        assertEquals(AppUpdateUrls.GITHUB_RELEASES, linker.getUpdateUrl())
    }

    @Test
    fun `returns GitHub releases when installer lookup throws`() {
        val context = RuntimeEnvironment.getApplication()
        val linker =
            AndroidAppUpdateLinker(context) {
                throw RuntimeException("PackageManager lookup failed")
            }

        assertEquals(AppUpdateUrls.GITHUB_RELEASES, linker.getUpdateUrl())
    }

    @Test
    fun `default installer lookup on API R+ returns GitHub releases under Robolectric`() {
        val context = RuntimeEnvironment.getApplication()
        val linker = AndroidAppUpdateLinker(context)

        assertEquals(AppUpdateUrls.GITHUB_RELEASES, linker.getUpdateUrl())
    }

    @Test
    @Config(sdk = [28])
    fun `default installer lookup on pre-R API returns GitHub releases under Robolectric`() {
        val context = RuntimeEnvironment.getApplication()
        val linker = AndroidAppUpdateLinker(context)

        assertEquals(AppUpdateUrls.GITHUB_RELEASES, linker.getUpdateUrl())
    }
}
