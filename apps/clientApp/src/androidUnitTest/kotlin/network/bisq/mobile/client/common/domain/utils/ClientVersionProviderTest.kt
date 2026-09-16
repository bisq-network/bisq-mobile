package network.bisq.mobile.client.common.domain.utils

import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.i18n.I18nSupport
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientVersionProviderTest {
    @Before
    fun setUpI18n() {
        I18nSupport.initialize("en")
    }

    @Test
    fun `version info names the tor daemon when the node is reached over tor`() {
        val provider = ClientVersionProvider(isTorRouted = { true })

        val info = provider.getVersionInfo(isDemo = false, isIOS = false)

        assertTrue(info.contains("Tor Daemon: ${BuildConfig.TOR_VERSION}"), info)
        assertTrue(info.contains("Api version: ${BuildConfig.BISQ_API_VERSION}"), info)
    }

    @Test
    fun `version info omits the tor daemon for a clearnet or LAN node`() {
        val provider = ClientVersionProvider(isTorRouted = { false })

        val info = provider.getVersionInfo(isDemo = false, isIOS = false)

        assertFalse(info.contains("Tor"), info)
        assertTrue(info.contains("Api version: ${BuildConfig.BISQ_API_VERSION}"), info)
    }

    @Test
    fun `the tor flag is read at call time, not at construction`() {
        var torRouted = false
        val provider = ClientVersionProvider(isTorRouted = { torRouted })
        assertFalse(provider.getVersionInfo(isDemo = false, isIOS = false).contains("Tor"))

        torRouted = true

        assertTrue(provider.getVersionInfo(isDemo = false, isIOS = false).contains("Tor Daemon"))
    }

    @Test
    fun `app name and version carry the demo prefix and the platform version`() {
        val provider = ClientVersionProvider(isTorRouted = { false })

        assertTrue(provider.getAppNameAndVersion(isDemo = true, isIOS = true).endsWith("vdemo-${BuildConfig.IOS_APP_VERSION}"))
        assertTrue(provider.getAppNameAndVersion(isDemo = false, isIOS = false).endsWith("v${BuildConfig.ANDROID_APP_VERSION}"))
    }
}
