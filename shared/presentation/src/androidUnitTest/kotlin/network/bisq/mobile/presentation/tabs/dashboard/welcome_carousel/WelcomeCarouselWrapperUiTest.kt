package network.bisq.mobile.presentation.tabs.dashboard.welcome_carousel

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exercises the top-level [WelcomeCarousel] wrapper: koinInject() of the presenter,
 * the primary-action routing (CarouselPageType → onRequest* callback), and the
 * empty-pages early return.
 */
@RunWith(AndroidJUnit4::class)
class WelcomeCarouselWrapperUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var presenter: WelcomeCarouselPresenter
    private lateinit var uiStateFlow: MutableStateFlow<WelcomeCarouselUiState>

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        uiStateFlow = MutableStateFlow(WelcomeCarouselUiState())
        presenter = mockk(relaxed = true)
        every { presenter.uiState } returns uiStateFlow

        startKoin {
            modules(
                module {
                    single<WelcomeCarouselPresenter> { presenter }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun renderCarousel(
        onRequestNotificationPermission: () -> Unit,
        onRequestBatteryOptimization: () -> Unit,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    WelcomeCarousel(
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onRequestBatteryOptimization = onRequestBatteryOptimization,
                    )
                }
            }
        }
    }

    @Test
    fun `tapping primary action on NOTIFICATIONS routes to notification request callback`() {
        var notifInvocations = 0
        var batteryInvocations = 0
        uiStateFlow.value = WelcomeCarouselUiState(pages = listOf(CarouselPageType.NOTIFICATIONS))

        renderCarousel(
            onRequestNotificationPermission = { notifInvocations++ },
            onRequestBatteryOptimization = { batteryInvocations++ },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.notifications.action".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, notifInvocations)
        assertEquals(0, batteryInvocations)
    }

    @Test
    fun `tapping primary action on BATTERY routes to battery request callback`() {
        var notifInvocations = 0
        var batteryInvocations = 0
        uiStateFlow.value = WelcomeCarouselUiState(pages = listOf(CarouselPageType.BATTERY))

        renderCarousel(
            onRequestNotificationPermission = { notifInvocations++ },
            onRequestBatteryOptimization = { batteryInvocations++ },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.battery.action".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, batteryInvocations)
        assertEquals(0, notifInvocations)
    }

    @Test
    fun `Don't ask again dispatches through presenter onAction`() {
        uiStateFlow.value = WelcomeCarouselUiState(pages = listOf(CarouselPageType.NOTIFICATIONS))

        renderCarousel(
            onRequestNotificationPermission = {},
            onRequestBatteryOptimization = {},
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.welcomeCarousel.dontAskAgain".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        verify(exactly = 1) {
            presenter.onAction(
                WelcomeCarouselUiAction.OnDontAskAgain(CarouselPageType.NOTIFICATIONS),
            )
        }
    }

    @Test
    fun `empty pages renders nothing observable`() {
        var notifInvocations = 0
        var batteryInvocations = 0
        uiStateFlow.value = WelcomeCarouselUiState(pages = emptyList())

        renderCarousel(
            onRequestNotificationPermission = { notifInvocations++ },
            onRequestBatteryOptimization = { batteryInvocations++ },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("mobile.welcomeCarousel.notifications.title".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("mobile.welcomeCarousel.battery.title".i18n())
            .assertCountEquals(0)
        assertFalse(notifInvocations > 0)
        assertTrue(batteryInvocations == 0)
    }
}
