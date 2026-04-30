package network.bisq.mobile.presentation.create_payment_account.account_review.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoneroAccountReviewContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(
        paymentMethod: CryptoPaymentMethodVO,
        account: MoneroAccount,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    MoneroAccountReviewContent(
                        paymentMethod = paymentMethod,
                        account = account,
                    )
                }
            }
        }
    }

    @Test
    fun `when direct address mode then header and direct address rows are shown`() {
        setTestContent(
            paymentMethod = sampleMethod(supportAutoConf = true),
            account = sampleAccount(useSubAddresses = false),
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Monero").assertIsDisplayed()
        composeTestRule.onNodeWithText("XMR").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when sub addresses enabled then direct address hidden and sub address rows shown`() {
        setTestContent(
            paymentMethod = sampleMethod(supportAutoConf = true),
            account =
                sampleAccount(
                    useSubAddresses = true,
                    mainAddress = "44AFFq5kSiGBoZ...",
                    privateViewKey = "0123456789abcdef",
                    accountIndex = 1,
                    initialSubAddressIndex = 2,
                    subAddress = "89ABCDE...",
                ),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.address".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.mainAddresses".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.privateViewKey".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.accountIndex".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.initialSubAddressIndex".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("89ABCDE...", substring = true)
            .assertCountEquals(1)
    }

    @Test
    fun `when auto conf supported but disabled then only toggle row shown`() {
        setTestContent(
            paymentMethod = sampleMethod(supportAutoConf = true),
            account = sampleAccount(isAutoConf = false),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.maxTradeAmount".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.explorerUrls".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when auto conf enabled then detail rows are shown`() {
        setTestContent(
            paymentMethod = sampleMethod(supportAutoConf = true),
            account =
                sampleAccount(
                    isAutoConf = true,
                    autoConfNumConfirmations = 2,
                    autoConfMaxTradeAmount = 1000,
                    autoConfExplorerUrls = "https://xmr.explorer",
                ),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.maxTradeAmount".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("https://xmr.explorer", substring = true)
            .assertCountEquals(1)
    }

    @Test
    fun `when auto conf not supported then auto conf rows are hidden`() {
        setTestContent(
            paymentMethod = sampleMethod(supportAutoConf = false),
            account = sampleAccount(isAutoConf = true),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when restrictions provided then restrictions row is shown and hidden when empty`() {
        var restrictions by mutableStateOf("Max. trade amount: 0.02 XMR")

        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    MoneroAccountReviewContent(
                        paymentMethod = sampleMethod(restrictions = restrictions),
                        account = sampleAccount(),
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.restrictions".i18n())
            .assertIsDisplayed()

        composeTestRule.runOnIdle {
            restrictions = ""
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.restrictions".i18n())
            .assertCountEquals(0)
    }

    private fun sampleMethod(
        supportAutoConf: Boolean = true,
        restrictions: String = "",
    ): CryptoPaymentMethodVO =
        CryptoPaymentMethodVO(
            paymentType = PaymentTypeVO.XMR,
            code = "XMR",
            name = "Monero",
            supportAutoConf = supportAutoConf,
            restrictions = restrictions,
        )

    private fun sampleAccount(
        useSubAddresses: Boolean = false,
        isAutoConf: Boolean? = null,
        mainAddress: String? = null,
        privateViewKey: String? = null,
        accountIndex: Int? = null,
        initialSubAddressIndex: Int? = null,
        subAddress: String? = null,
        autoConfNumConfirmations: Int? = null,
        autoConfMaxTradeAmount: Long? = null,
        autoConfExplorerUrls: String? = null,
    ): MoneroAccount =
        MoneroAccount(
            accountName = "Monero Main",
            accountPayload =
                MoneroAccountPayload(
                    address = "48A_MAIN_ADDRESS",
                    isInstant = false,
                    isAutoConf = isAutoConf,
                    autoConfNumConfirmations = autoConfNumConfirmations,
                    autoConfMaxTradeAmount = autoConfMaxTradeAmount,
                    autoConfExplorerUrls = autoConfExplorerUrls,
                    useSubAddresses = useSubAddresses,
                    mainAddress = mainAddress,
                    privateViewKey = privateViewKey,
                    subAddress = subAddress,
                    accountIndex = accountIndex,
                    initialSubAddressIndex = initialSubAddressIndex,
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )
}
