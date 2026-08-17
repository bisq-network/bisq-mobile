package network.bisq.mobile.presentation.main

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import network.bisq.mobile.presentation.common.ui.navigation.ExternalUriHandler
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.module.Module
import org.koin.dsl.module
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class MainActivityDeepLinkTest : PresentationKoinTestBase() {
    private val presenter = mockk<MainPresenter>(relaxed = true)

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single { presenter }
            },
        )

    override fun onSetup() {
        MainApplication.wasProcessDead.set(false)
        clearCachedUri()
    }

    override fun onTearDown() {
        try {
            clearCachedUri()
        } finally {
            super.onTearDown()
        }
    }

    @Test
    fun `onCreate caches deep link and strips it from activity intent`() {
        val deepLinkIntent = createDeepLinkIntent("bisq://trade/123")

        val activity =
            Robolectric
                .buildActivity(TestMainActivity::class.java, deepLinkIntent)
                .setup()
                .get()

        assertNull(activity.intent.action)
        assertNull(activity.intent.data)
        assertNull(activity.intent.clipData)
        assertEquals("bisq://trade/123", consumeCachedUri())
        assertNull(consumeCachedUri())
    }

    @Test
    fun `onNewIntent caches deep link and updates activity intent with sanitized copy`() {
        val activity =
            Robolectric
                .buildActivity(TestMainActivity::class.java, Intent(Intent.ACTION_MAIN))
                .setup()
                .get()

        val deepLinkIntent = createDeepLinkIntent("bisq://openTrade?id=7")

        activity.dispatchNewIntent(deepLinkIntent)

        assertNull(activity.intent.action)
        assertNull(activity.intent.data)
        assertNull(activity.intent.clipData)
        assertEquals("bisq://openTrade?id=7", consumeCachedUri())
        assertNull(consumeCachedUri())
    }

    @Test
    fun `non deeplink intents remain intact and do not populate external uri cache`() {
        val launchIntent =
            Intent(Intent.ACTION_MAIN).apply {
                `package` = "network.bisq.mobile.test"
            }

        val activity =
            Robolectric
                .buildActivity(TestMainActivity::class.java, launchIntent)
                .setup()
                .get()

        assertEquals(Intent.ACTION_MAIN, activity.intent.action)
        assertNotNull(activity.intent.`package`)
        assertNull(consumeCachedUri())
    }

    private fun createDeepLinkIntent(uri: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            clipData = ClipData.newPlainText("deeplink", uri)
        }

    private fun clearCachedUri() {
        consumeCachedUri()
        ExternalUriHandler.listener = null
    }

    private fun consumeCachedUri(): String? {
        var cachedUri: String? = null
        ExternalUriHandler.listener = { uri -> cachedUri = uri }
        ExternalUriHandler.listener = null
        return cachedUri
    }
}

internal class TestMainActivity : MainActivity() {
    fun dispatchNewIntent(intent: Intent) {
        onNewIntent(intent)
    }
}
