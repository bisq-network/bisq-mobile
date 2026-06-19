package network.bisq.mobile.presentation.settings.payment_accounts

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.di.presentationTestModule
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
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

@RunWith(AndroidJUnit4::class)
class PaymentAccountsScreenWrapperUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var presenter: PaymentAccountsPresenter

    @Before
    fun setup() {
        I18nSupport.setLanguage()
        presenter = mockk(relaxed = true)
        every { presenter.uiState } returns MutableStateFlow(PaymentAccountsUiState())
        every { presenter.isAddAccountEnabled } returns MutableStateFlow(true)
        every { presenter.isSaveAccountEnabled } returns MutableStateFlow(true)
        every { presenter.isDeleteAccountEnabled } returns MutableStateFlow(true)

        startKoin {
            modules(
                module {
                    single<PaymentAccountsPresenter> { presenter }
                    single<ITopBarPresenter> { PreviewTopBarPresenter() }
                },
                presentationTestModule,
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `PaymentAccountsScreen collects presenter guard state and renders content`() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    PaymentAccountsScreen()
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.noAccounts.info".i18n())
            .assertIsDisplayed()
    }
}
