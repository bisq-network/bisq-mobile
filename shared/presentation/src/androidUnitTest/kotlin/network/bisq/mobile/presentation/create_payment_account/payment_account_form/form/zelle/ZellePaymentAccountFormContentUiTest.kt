package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.ZelleFormUiAction
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ZellePaymentAccountFormContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: ZelleFormPresenter

    private val samplePaymentMethod: FiatPaymentMethodVO =
        FiatPaymentMethodVO(
            paymentType = PaymentTypeVO.ZELLE,
            name = "Zelle",
            supportedCurrencyCodes = "USD",
            countryNames = "United States",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
        mainPresenter = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                },
            )
        }

        presenter = ZelleFormPresenter(mainPresenter)
    }

    @After
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun setTestContent(
        onNavigateToNextScreen: (PaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    ZellePaymentAccountFormContent(
                        presenter = presenter,
                        paymentMethod = samplePaymentMethod,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                    )
                }
            }
        }
    }

    @Test
    fun `when wrapper is composed then it initializes presenter with payment method`() {
        setTestContent()

        composeTestRule.waitForIdle()

        assertEquals(samplePaymentMethod, presenter.paymentMethod)
    }

    @Test
    fun `when presenter emits next effect then wrapper navigates with account`() {
        var navigatedAccount: PaymentAccount? = null

        setTestContent(onNavigateToNextScreen = { navigatedAccount = it })
        composeTestRule.waitForIdle()

        presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Zelle Personal"))
        presenter.onAction(ZelleFormUiAction.OnHolderNameChange("John Doe"))
        presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("john@example.com"))

        composeTestRule.runOnIdle {
            presenter.onAction(AccountFormUiAction.OnNextClick)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { navigatedAccount != null }
        assertNotNull(navigatedAccount)
        assertEquals("Zelle Personal", navigatedAccount.accountName)
    }
}
