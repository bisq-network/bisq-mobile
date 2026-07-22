package network.bisq.mobile.data.utils

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AndroidAppUpdateLinkerTest {
    private companion object {
        private const val GOOGLE_PLAY_INSTALLER = "com.android.vending"
        private const val GOOGLE_PLAY_FEEDBACK = "com.google.android.feedback"
    }

    @Test
    fun `returns Play Store URL when installed from Google Play`() {
        val context = RuntimeEnvironment.getApplication()
        val linker =
            AndroidAppUpdateLinker(context) { GOOGLE_PLAY_INSTALLER }

        assertEquals(
            AppUpdateUrls.playStoreDetailsUrl(context.packageName),
            linker.getUpdateUrl(),
        )
    }

    @Test
    fun `returns Play Store URL when installed from Google Play feedback installer`() {
        val context = RuntimeEnvironment.getApplication()
        val linker =
            AndroidAppUpdateLinker(context) { GOOGLE_PLAY_FEEDBACK }

        assertEquals(
            AppUpdateUrls.playStoreDetailsUrl(context.packageName),
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
}
