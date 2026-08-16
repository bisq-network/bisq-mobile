package network.bisq.mobile.presentation.settings.ignored_users

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Covers the stateful [IgnoredUsersScreen] — the seam [IgnoredUsersContentUiTest] cannot reach:
 * the presenter resolved from Koin, its load firing from the lifecycle, and actions round-tripping
 * back into the facade.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IgnoredUsersScreenUiTest : PresentationKoinComposeTestBase() {
    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var mainPresenter: MainPresenter

    private val peer = createMockUserProfile(PEER_ID)

    private companion object {
        /** [createMockUserProfile] sets `networkId.pubKey.id` to the name, so the id is the name. */
        const val PEER_ID = "SatoshiFan"
    }

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
                factory { IgnoredUsersPresenter(userProfileServiceFacade, mainPresenter) }
            },
        )

    override fun onKoinReady() {
        super.onKoinReady()
        userProfileServiceFacade = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } returns setOf(PEER_ID)
        coEvery { userProfileServiceFacade.findUserProfiles(any()) } returns listOf(peer)
    }

    @Test
    fun `when attached then renders the ignored peers loaded from the facade`() {
        setTestContent { IgnoredUsersScreen() }

        composeTestRule.onNodeWithText(peer.userName).assertIsDisplayed()
    }

    @Test
    fun `when the load fails then shows the error and retry reloads`() {
        coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } throws RuntimeException("no connection")

        setTestContent { IgnoredUsersScreen() }

        composeTestRule
            .onNodeWithText("mobile.settings.ignoredUsers.loadFailed".i18n())
            .assertIsDisplayed()

        coEvery { userProfileServiceFacade.getIgnoredUserProfileIds() } returns setOf(PEER_ID)
        composeTestRule.onNodeWithText("mobile.action.retry".i18n()).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(peer.userName).assertIsDisplayed()
    }

    @Test
    fun `unblocking a peer confirms through the facade`() {
        setTestContent { IgnoredUsersScreen() }

        composeTestRule
            .onNodeWithText("mobile.settings.ignoredUsers.unblock".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) { userProfileServiceFacade.undoIgnoreUserProfile(PEER_ID) }
    }
}
