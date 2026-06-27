package network.bisq.mobile.presentation.settings.faqs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FaqScreenContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockOnAction: (FaqUiAction) -> Unit
    private lateinit var faqUiState: FaqUiState

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        mockOnAction = mockk(relaxed = true)
        faqUiState = sampleFaqUiState()
    }

    private fun sampleFaqUiState(): FaqUiState =
        FaqUiState(
            faqs =
                listOf(
                    FaqItemUiState(
                        question = "mobile.faqs.q1.question".i18n(),
                        answer = "mobile.faqs.q1.answer".i18n(),
                    ),
                    FaqItemUiState(
                        question = "mobile.faqs.q2.question".i18n(),
                        answer = "mobile.faqs.q2.answer.connect".i18n(),
                    ),
                    FaqItemUiState(
                        question = "mobile.faqs.q3.question".i18n(),
                        answer = "mobile.faqs.q3.answer.connect".i18n(),
                    ),
                ),
        )

    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    content()
                }
            }
        }
    }

    @Test
    fun `when faq content renders then shows header questions and footer link`() {
        setTestContent {
            FaqScreenContent(
                uiState = faqUiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("mobile.faqs.header.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.faqs.header.subtitle".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.faqs.q1.question".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.faqs.q2.question".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.faqs.wantToKnowMore".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when faq question clicked then answer expands`() {
        setTestContent {
            FaqScreenContent(
                uiState = faqUiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("mobile.faqs.q1.answer".i18n()).assertDoesNotExist()

        composeTestRule
            .onNodeWithText("mobile.faqs.q1.question".i18n())
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("mobile.faqs.q1.answer".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when expanded faq question clicked again then answer collapses`() {
        setTestContent {
            FaqScreenContent(
                uiState = faqUiState,
                onAction = mockOnAction,
                initialExpandedIndex = 0,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.faqs.q1.answer".i18n()).assertIsDisplayed()

        composeTestRule
            .onNodeWithText("mobile.faqs.q1.question".i18n())
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("mobile.faqs.q1.answer".i18n()).assertDoesNotExist()
    }

    @Test
    fun `when second faq question clicked then only second answer is shown`() {
        setTestContent {
            FaqScreenContent(
                uiState = faqUiState,
                onAction = mockOnAction,
                initialExpandedIndex = 0,
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.faqs.q2.question".i18n())
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("mobile.faqs.q1.answer".i18n()).assertDoesNotExist()
        composeTestRule.onNodeWithText("mobile.faqs.q2.answer.connect".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when want to know more clicked then triggers action`() {
        setTestContent {
            FaqScreenContent(
                uiState = faqUiState,
                onAction = mockOnAction,
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("mobile.faqs.wantToKnowMore".i18n())
            .performScrollTo()
            .performClick()

        verify { mockOnAction(FaqUiAction.OnWantToKnowMoreClick) }
    }
}
