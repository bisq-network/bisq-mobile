package network.bisq.mobile.presentation.settings.payment_accounts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountsScreenUiTest : PresentationKoinComposeTestBase() {
    private lateinit var presenter: PaymentAccountsPresenter

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<PaymentAccountsPresenter> { presenter }
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
            },
        )

    override fun onKoinReady() {
        presenter = mockk(relaxed = true)
        every { presenter.uiState } returns MutableStateFlow(PaymentAccountsUiState())
        every { presenter.isAddAccountEnabled } returns MutableStateFlow(true)
        every { presenter.isSaveAccountEnabled } returns MutableStateFlow(true)
        every { presenter.isDeleteAccountEnabled } returns MutableStateFlow(true)
    }

    @Test
    fun `PaymentAccountsScreen collects presenter guard state and renders content`() {
        setTestContent { PaymentAccountsScreen() }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.noAccounts.info".i18n())
            .assertIsDisplayed()
    }
}
