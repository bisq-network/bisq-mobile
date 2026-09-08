package network.bisq.mobile.presentation.community

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.data.replicated.chat.common.CommonPublicChatChannel
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.chat.public_chat.PublicChatServiceFacade
import network.bisq.mobile.data.service.contacts.ContactsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.community.contacts.ContactsPresenter
import network.bisq.mobile.presentation.community.public_chat.PublicChatPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.fixtures.DISCUSSION_MESSAGE_TEXT
import network.bisq.mobile.test.fixtures.SUPPORT_MESSAGE_TEXT
import network.bisq.mobile.test.fixtures.testCommunityHubService
import network.bisq.mobile.test.fixtures.testPublicChatChannel
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.compose.PresentationInjectComposeUiTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The hub's segment behaviour as the real screen wires it: which domain Discussions is mounted on,
 * and which segment survives a trip forward and back. `CommunityHubScreenUiTest` drives the
 * stateless content with a stand-in body and no presenter, so neither is observable there.
 *
 * The domain half mirrors `SupportChannelScreenUiTest` from the other side: the facade serves both
 * channels, so the wrong domain renders a plausible thread of the wrong conversation under the
 * Discussions label — no error, no spinner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityHubScreenSegmentUiTest : PresentationInjectComposeUiTestBase() {
    private lateinit var publicChatServiceFacade: PublicChatServiceFacade
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var contactsServiceFacade: ContactsServiceFacade
    private lateinit var mainPresenter: MainPresenter

    private val alice: UserProfileVO = createMockUserProfile("alice")
    private val channels = MutableStateFlow<List<CommonPublicChatChannel>>(emptyList())
    private val screenMounted = mutableStateOf(true)

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
                factory {
                    CommunityHubPresenter(
                        mainPresenter,
                        testCommunityHubService(
                            enabled = setOf(CommunitySegment.DISCUSSIONS, CommunitySegment.CONTACTS),
                            // Both segments have to be live for this class to say anything about
                            // segment bodies, and the fixture's default requirements come from
                            // production — so the day DISCUSSIONS declares one, every test here
                            // would go red on the capability gate instead.
                            requiredFeatures = emptyMap(),
                            dispatcher = UnconfinedTestDispatcher(testDispatcher.scheduler),
                        ),
                    )
                }
                factory { ContactsPresenter(mainPresenter, contactsServiceFacade, userProfileServiceFacade) }
                factory { params ->
                    PublicChatPresenter(
                        mainPresenter,
                        publicChatServiceFacade,
                        userProfileServiceFacade,
                        // The chat-rules warn box seeded away — the state of anyone who has tapped
                        // "Don't show again". Not cosmetic here: Robolectric's default viewport is
                        // ~320x470dp, and the hub already stacks a top bar, a tab row, the Support
                        // row and the search field above the thread, so with the box on top of those
                        // the message list is laid out at zero height and nothing is displayed.
                        SettingsRepositoryMock(Settings(showChatRulesWarnBox = false)),
                        params.get(),
                    )
                }
            },
        )

    override fun onKoinReady() {
        super.onKoinReady()
        publicChatServiceFacade = mockk(relaxed = true)
        userProfileServiceFacade = mockk(relaxed = true)
        contactsServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        every { contactsServiceFacade.contacts } returns MutableStateFlow(emptyList())
        every { contactsServiceFacade.isLoaded } returns MutableStateFlow(true)

        every { publicChatServiceFacade.channels } returns channels
        every { publicChatServiceFacade.isSupported } returns flowOf(true)
        every { userProfileServiceFacade.ignoredProfileIds } returns MutableStateFlow(emptySet())
        coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(alice)

        channels.value =
            listOf(
                // Support first, so dropping the domain predicate in `awaitChannel` reddens this
                // test on its own rather than leaning on its Support-side twin, which seeds the
                // other order.
                testPublicChatChannel(ChatChannelDomainEnum.SUPPORT, SUPPORT_MESSAGE_TEXT, alice),
                testPublicChatChannel(ChatChannelDomainEnum.DISCUSSION, DISCUSSION_MESSAGE_TEXT, alice),
            )
    }

    /** The thread renders link text, which asks the composition for an opener it cannot default. */
    private fun setCommunityHubScreen(initialSegment: CommunitySegment? = null) =
        setInjectTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
                if (screenMounted.value) CommunityHubScreen(initialSegment = initialSegment)
            }
        }

    /**
     * What navigating forward and back does to this screen: the destination leaves composition while
     * its `NavBackStackEntry` — and so the [viewModelStore] the presenter is cached in — stays alive.
     * The presenter that comes back is the same instance; every `remember` slot around it is new.
     */
    private fun navigateAwayAndBack() {
        screenMounted.value = false
        composeTestRule.waitForIdle()
        screenMounted.value = true
        composeTestRule.waitForIdle()
    }

    @Test
    fun `the discussions segment renders the discussion thread and not the support one`() {
        setCommunityHubScreen()

        composeTestRule.onNodeWithText(DISCUSSION_MESSAGE_TEXT).assertIsDisplayed()
        composeTestRule.onNodeWithText(SUPPORT_MESSAGE_TEXT).assertDoesNotExist()
    }

    /**
     * A deep link picks the segment the hub opens on; it must not keep picking it. More → My Contacts
     * pushes `CommunityHub(initialSegment = CONTACTS)`, and the route argument outlives every
     * composition of that back-stack entry — so without a once-only guard, coming back from the
     * Support screen drops the user on Contacts instead of the Discussions tab they left.
     */
    @Test
    fun `a deep-linked segment is not re-applied when the screen comes back`() {
        setCommunityHubScreen(initialSegment = CommunitySegment.CONTACTS)

        composeTestRule.onNodeWithText("mobile.community.tab.discussions".i18n()).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(DISCUSSION_MESSAGE_TEXT).assertIsDisplayed()

        navigateAwayAndBack()

        composeTestRule.onNodeWithText(DISCUSSION_MESSAGE_TEXT).assertIsDisplayed()
    }
}
