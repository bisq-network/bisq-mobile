package network.bisq.mobile.presentation.community.public_chat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.fixtures.DISCUSSION_MESSAGE_TEXT
import network.bisq.mobile.test.fixtures.SUPPORT_MESSAGE_TEXT
import network.bisq.mobile.test.fixtures.testPublicChatChannel
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The Support screen shares every moving part with the Discussions tab except the domain it is
 * mounted on, so that domain is what this covers: the facade serves both channels, and picking the
 * wrong one renders a plausible thread of the wrong conversation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SupportChannelScreenUiTest : PresentationKoinComposeTestBase() {
    private lateinit var publicChatServiceFacade: PublicChatServiceFacade
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var mainPresenter: MainPresenter

    private val alice: UserProfileVO = createMockUserProfile("alice")
    private val analyticsService: AnalyticsService = mockk(relaxed = true)
    private val channels = MutableStateFlow<List<CommonPublicChatChannel>>(emptyList())

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
                single<AnalyticsService> { analyticsService }
                factory { params ->
                    PublicChatPresenter(
                        mainPresenter,
                        publicChatServiceFacade,
                        userProfileServiceFacade,
                        SettingsRepositoryMock(),
                        params.get(),
                    )
                }
            },
        )

    override fun onKoinReady() {
        super.onKoinReady()
        publicChatServiceFacade = mockk(relaxed = true)
        userProfileServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        every { publicChatServiceFacade.channels } returns channels
        every { publicChatServiceFacade.isSupported } returns flowOf(true)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        every { userProfileServiceFacade.userProfiles } returns MutableStateFlow(emptyList())
        coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(alice)

        channels.value =
            listOf(
                // Discussion first, so a thread that takes the first channel instead of the one
                // matching its domain reddens this test rather than leaning on its hub-side twin,
                // which seeds the other order.
                testPublicChatChannel(ChatChannelDomainEnum.DISCUSSION, DISCUSSION_MESSAGE_TEXT, alice),
                testPublicChatChannel(ChatChannelDomainEnum.SUPPORT, SUPPORT_MESSAGE_TEXT, alice),
            )
    }

    /** The thread renders link text, which asks the composition for an opener it cannot default. */
    private fun setSupportChannelScreen() =
        setTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
                SupportChannelScreen()
            }
        }

    @Test
    fun `the screen renders the support thread and not the discussion one`() {
        setSupportChannelScreen()

        composeTestRule.onNodeWithText(SUPPORT_MESSAGE_TEXT).assertIsDisplayed()
        composeTestRule.onNodeWithText(DISCUSSION_MESSAGE_TEXT).assertDoesNotExist()
    }

    /**
     * The screen event is read off the presenter's domain, so it exists only once the screen has set
     * one. Nothing else in this diff renders the screen with an analytics service bound, which makes
     * this the only place the order between setting the domain and attaching the view is observable.
     */
    @Test
    fun `the screen reports itself as the support screen`() {
        setSupportChannelScreen()

        verify { analyticsService.track(AnalyticsEvent.ScreenOpened.CommunitySupport) }
    }

    /** Pushed as its own screen, so it carries the chrome the hub supplies for the Discussions tab. */
    @Test
    fun `the screen carries the support channel title`() {
        setSupportChannelScreen()

        composeTestRule.onNodeWithText("chat.channelDomain.SUPPORT".i18n()).assertIsDisplayed()
    }
}
