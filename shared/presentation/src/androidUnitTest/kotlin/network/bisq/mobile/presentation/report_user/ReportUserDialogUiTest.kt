package network.bisq.mobile.presentation.report_user

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.Test

/**
 * The dialog seeds its text field from `reportMessage`, the draft kept after a failed report. That
 * seeding must happen once per dialog and never again while the user is typing a replacement.
 */
class ReportUserDialogUiTest : PresentationKoinComposeTestBase() {
    private val userProfileServiceFacade: UserProfileServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val accusedUserProfile = createMockUserProfile("Satoshi")

    /** Written to by the test's callbacks, purely so those lambdas capture `tick`. */
    private var lastTick: Int = -1

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                factory { ReportUserPresenter(mainPresenter, userProfileServiceFacade) }
            },
        )

    /**
     * Both callers pass inline lambdas capturing their presenter, so the callbacks take a new
     * identity on any recomposition of the host screen. Keying the dialog's `LaunchedEffect` on them
     * restarts it, re-runs `initialize`, and re-seeds the stale draft over the in-progress edit —
     * which is what bumping `tick` below reproduces.
     */
    @Test
    fun `when the host recomposes with new callbacks then the typed message survives`() {
        val tick = mutableIntStateOf(0)

        setTestContent {
            // Read during composition, then captured *by value* in the callbacks: that is what gives
            // them a new identity per recomposition. Reading the state inside the lambda body
            // instead would leave the captures stable, Compose would memoise them, and the dialog
            // would be skipped entirely — the trap this test exists to spring.
            val currentTick = tick.intValue

            ReportUserDialog(
                accusedUserProfile = accusedUserProfile,
                reportMessage = STALE_DRAFT,
                onReportFailure = { _ -> lastTick = currentTick },
                onReportSuccess = { lastTick = currentTick },
            )
        }

        composeTestRule.onNodeWithText(STALE_DRAFT).assertIsDisplayed()
        composeTestRule.onNodeWithText(STALE_DRAFT).performTextReplacement(EDITED_DRAFT)
        composeTestRule.waitForIdle()

        tick.intValue++
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(EDITED_DRAFT).assertIsDisplayed()
    }

    /**
     * `UserProfileVO` is a data class, so a peer republishing their profile mid-report arrives as an
     * unequal value for the same peer. Keying the effect on the whole VO would restart it there and
     * re-seed the stale draft; keying on the id — which is what identifies a peer — does not.
     */
    @Test
    fun `when the peer republishes their profile then the typed message survives`() {
        val profile = mutableStateOf(accusedUserProfile)

        setTestContent {
            ReportUserDialog(
                accusedUserProfile = profile.value,
                reportMessage = STALE_DRAFT,
            )
        }

        composeTestRule.onNodeWithText(STALE_DRAFT).assertIsDisplayed()
        composeTestRule.onNodeWithText(STALE_DRAFT).performTextReplacement(EDITED_DRAFT)
        composeTestRule.waitForIdle()

        // Same id, different content — exactly what a re-published profile looks like.
        profile.value = accusedUserProfile.copy(statement = "freshly edited statement")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(EDITED_DRAFT).assertIsDisplayed()
    }

    @Test
    fun `a report opened from a message sends the message metadata`() {
        coEvery { userProfileServiceFacade.reportUserProfile(any(), any()) } returns Result.success(Unit)

        setTestContent {
            ReportUserDialog(
                accusedUserProfile = accusedUserProfile,
                reportMessage = STALE_DRAFT,
                reportedMessage = REPORTED_MESSAGE,
            )
        }

        composeTestRule.onNode(hasText("chat.reportToModerator.report".i18n()) and hasClickAction()).performClick()
        composeTestRule.waitForIdle()

        coVerify {
            userProfileServiceFacade.reportUserProfile(accusedUserProfile, REPORTED_MESSAGE.appendTo(STALE_DRAFT))
        }
    }

    private companion object {
        const val STALE_DRAFT = "kept after the failed report"
        const val EDITED_DRAFT = "what the user is typing now"
        val REPORTED_MESSAGE = ReportedMessage(channel = "discussion.bisq", date = 0, text = "buy my coin")
    }
}
