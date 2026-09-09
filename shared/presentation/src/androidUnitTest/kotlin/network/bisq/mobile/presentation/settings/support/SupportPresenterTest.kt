package network.bisq.mobile.presentation.settings.support

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.domain.service.capabilities.BackendCapabilities
import network.bisq.mobile.domain.service.capabilities.Feature
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.fixtures.testCommunityHubService
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Help screen's in-app Support entry. The external links it lists always work; the in-app one
 * only does in a build that serves public chat, so availability is the part worth covering.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SupportPresenterTest : PresentationKoinTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private val capabilities = MutableStateFlow(BackendCapabilities.UNAVAILABLE)

    override fun onKoinReady() {
        // onViewAttached resolves the GitHub issue body, and PresentationKoinTestBase does not set
        // up i18n the way the Compose bases do.
        I18nSupport.setLanguage()
        mainPresenter = mockk(relaxed = true)
    }

    private fun TestScope.createPresenter(
        enabled: Set<CommunitySegment>,
        requiredFeatures: Map<CommunitySegment, Feature> = emptyMap(),
    ): SupportPresenter {
        val hubService =
            testCommunityHubService(
                enabled = enabled,
                requiredFeatures = requiredFeatures,
                capabilities = capabilities,
                dispatcher = UnconfinedTestDispatcher(testScheduler),
            )
        return SupportPresenter(
            mainPresenter,
            mockk<VersionProvider>(relaxed = true),
            mockk<DeviceInfoProvider>(relaxed = true),
            hubService,
        )
    }

    private fun TestScope.createAttachedPresenter(
        enabled: Set<CommunitySegment>,
        requiredFeatures: Map<CommunitySegment, Feature> = emptyMap(),
    ): SupportPresenter =
        createPresenter(enabled, requiredFeatures).also {
            it.onViewAttached()
            advanceUntilIdle()
        }

    @Test
    fun `the support channel is offered when discussions is live`() =
        runTest {
            val presenter = createAttachedPresenter(enabled = setOf(CommunitySegment.DISCUSSIONS))

            assertTrue(presenter.uiState.value.isSupportChannelAvailable)
        }

    /**
     * Availability has to be right on the first frame, not one recomposition later. `SupportPresenter`
     * is a Koin `factory` behind `RememberPresenterLifecycle`, so the Help screen builds a fresh one
     * every time it enters composition — every return from the Support channel included. A flag that
     * only fills in `onViewAttached` makes the entry pop in after the list has already been laid out
     * without it. `liveSegments` is `stateIn(Eagerly)` and has a value at construction, so there is
     * nothing to wait for.
     */
    @Test
    fun `the support channel is offered before the view attaches`() =
        runTest {
            val presenter = createPresenter(enabled = setOf(CommunitySegment.DISCUSSIONS))

            assertTrue(presenter.uiState.value.isSupportChannelAvailable)
        }

    /**
     * Discussions carries the public chat rollout, so without it the link would push a thread the
     * app cannot fill — a spinner, not an error.
     */
    @Test
    fun `the support channel is withheld when discussions is not live`() =
        runTest {
            val presenter = createAttachedPresenter(enabled = setOf(CommunitySegment.CONTACTS))

            assertFalse(presenter.uiState.value.isSupportChannelAvailable)
        }

    /**
     * The flag has to follow the flow, not sample it once. `isSupportChannelAvailable` starts false,
     * so the withheld case alone would pass just as well on a presenter that never collected —
     * dropping the segment out from under an attached one is what separates the two.
     */
    @Test
    fun `the support channel is withdrawn when discussions stops being live`() =
        runTest {
            capabilities.value = BackendCapabilities(setOf(Feature.PRIVATE_CHAT.key))
            val presenter =
                createAttachedPresenter(
                    enabled = setOf(CommunitySegment.DISCUSSIONS),
                    // Any feature does here: what is under test is that the flag tracks liveSegments, and
                    // a backend requirement is the only thing that moves that set after construction.
                    requiredFeatures = mapOf(CommunitySegment.DISCUSSIONS to Feature.PRIVATE_CHAT),
                )
            assertTrue(presenter.uiState.value.isSupportChannelAvailable)

            capabilities.value = BackendCapabilities.UNAVAILABLE
            advanceUntilIdle()

            assertFalse(presenter.uiState.value.isSupportChannelAvailable)
        }

    @Test
    fun `opening the support channel navigates to it`() =
        runTest {
            val presenter = createAttachedPresenter(enabled = setOf(CommunitySegment.DISCUSSIONS))

            presenter.onAction(SupportUiAction.OnOpenSupportChannel)
            advanceUntilIdle()

            verify { navigationManager.navigate(NavRoute.SupportChannel, any(), any()) }
        }
}
