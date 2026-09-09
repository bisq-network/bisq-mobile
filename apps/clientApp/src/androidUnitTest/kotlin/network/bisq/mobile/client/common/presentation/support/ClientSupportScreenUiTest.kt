package network.bisq.mobile.client.common.presentation.support

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.client.common.test_utils.ClientInjectComposeUiTestBase
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.settings.support.SupportPresenter
import network.bisq.mobile.test.fixtures.testCommunityHubService
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Connect keeps its own copy of the Help screen, so the shared screen's coverage says nothing about
 * this one. What rots here is the copy falling behind: the entry point #1746 asks for has to be on
 * both, and Connect is where it will one day matter most.
 */
class ClientSupportScreenUiTest : ClientInjectComposeUiTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var clientPresenter: ClientSupportPresenter
    private var enabledSegments: Set<CommunitySegment> = emptySet()

    private val openChannelLabel = "mobile.community.support.openChannel".i18n()

    private companion object {
        const val DEBUG_PANEL_HEADLINE = "Push Notifications (Debug)"
    }

    override fun onBeforeKoinStart() {
        mainPresenter = mockk(relaxed = true)
        clientPresenter = mockk(relaxed = true)
        // The debug panel branches on this. A relaxed mock hands back an erased Object for a
        // StateFlow, which only blows up where the value is actually read — so leaving it
        // unstubbed is a ClassCastException that fires in the debug build and nowhere else.
        every { clientPresenter.uiState } returns MutableStateFlow(ClientSupportUiState())
    }

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                factory { clientPresenter }
                factory {
                    SupportPresenter(
                        mainPresenter,
                        mockk<VersionProvider>(relaxed = true),
                        mockk<DeviceInfoProvider>(relaxed = true),
                        // This base shares no TestCoroutineScheduler, so there is nothing to hand
                        // the fixture; its own default test dispatcher is what keeps the service's
                        // stateIn scope off Dispatchers.Default.
                        testCommunityHubService(
                            enabled = enabledSegments,
                            // The rollout flag is the axis under test; see SupportScreenUiTest.
                            requiredFeatures = emptyMap(),
                        ),
                    )
                }
            },
        )

    private fun setClientSupportScreen(isDebug: Boolean = false) =
        setInjectTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
                ClientSupportScreen(isDebug = isDebug)
            }
        }

    @Test
    fun `the support channel entry is offered when discussions is live`() {
        enabledSegments = setOf(CommunitySegment.DISCUSSIONS)

        setClientSupportScreen()

        composeTestRule.onNodeWithText(openChannelLabel).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.support.communityChannels".i18n()).assertIsDisplayed()
    }

    /**
     * `BuildConfig.IS_DEBUG` is a const whose value follows the gradle invocation, so whichever way
     * it lands one of the two branches is dead code for the whole run — under `testDebugUnitTest` it
     * is the release one. The `isDebug` seam is what lets both be covered anyway, and these two are
     * what say it is wired the way round it claims: a release build must not ship a device-token
     * dump.
     */
    @Test
    fun `the push notification debug panel is shown in a debug build`() {
        setClientSupportScreen(isDebug = true)

        composeTestRule.onNodeWithText(DEBUG_PANEL_HEADLINE).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the push notification debug panel is absent in a release build`() {
        setClientSupportScreen(isDebug = false)

        composeTestRule.onNodeWithText(DEBUG_PANEL_HEADLINE).assertDoesNotExist()
    }

    @Test
    fun `the support channel entry is withheld when discussions is not live`() {
        enabledSegments = emptySet()

        setClientSupportScreen()

        composeTestRule.onNodeWithText(openChannelLabel).assertDoesNotExist()
        composeTestRule.onNodeWithText("mobile.support.communityChannels".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.support.matrix".i18n()).assertIsDisplayed()
    }
}
