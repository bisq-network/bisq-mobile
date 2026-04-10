package network.bisq.mobile.presentation.settings.payment_accounts_musig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSegmentButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.CryptoAccountVO
import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.FiatAccountVO
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.CryptoPaymentAccountCard
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.FiatPaymentAccountCard

enum class PaymentAccountTab(
    val titleKey: String,
) {
    FIAT("mobile.user.paymentAccounts.fiat"),
    CRYPTO("mobile.user.paymentAccounts.crypto"),
}

@ExcludeFromCoverage
@Composable
fun PaymentAccountsMusigScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<PaymentAccountsMusigPresenter>()

    val uiState by presenter.uiState.collectAsState()

    PaymentAccountsMusigContent(
        uiState = uiState,
        onAction = presenter::onAction,
        topBar = { TopBar("paymentAccounts.headline".i18n()) },
    )
}

@Composable
fun PaymentAccountsMusigContent(
    uiState: PaymentAccountsMusigUiState,
    onAction: (PaymentAccountsMusigUiAction) -> Unit,
    topBar: @Composable () -> Unit = {},
) {
    BisqScaffold(
        topBar = topBar,
    ) { paddingValues ->

        when {
            uiState.isLoadingAccounts -> {
                LoadingState(paddingValues)
            }

            uiState.isLoadingAccountsError -> {
                ErrorState(
                    paddingValues = paddingValues,
                    onRetry = { onAction(PaymentAccountsMusigUiAction.OnRetryLoadAccountsClick) },
                )
            }

            else -> {
                AccountsLoadedState(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onAction = onAction,
                )
            }
        }

        if (uiState.showDeleteConfirmationDialog) {
            ConfirmationDialog(
                onConfirm = { onAction(PaymentAccountsMusigUiAction.OnConfirmDeleteAccountClick) },
                onDismiss = { onAction(PaymentAccountsMusigUiAction.OnCancelDeleteAccountClick) },
            )
        }
    }
}

@Composable
private fun EmptyAccountsInfoSection() {
    Column(
        modifier =
            Modifier
                .verticalScroll(rememberScrollState()),
    ) {
        BisqText.H4LightGrey("paymentAccounts.noAccounts.info".i18n())
        BisqGap.V2()
        BisqText.H2Light("paymentAccounts.noAccounts.whySetup".i18n())
        BisqGap.V1()
        BisqText.BaseLight("paymentAccounts.noAccounts.whySetup.info".i18n())
        BisqGap.V2()
        BisqText.BaseLightGrey("paymentAccounts.noAccounts.whySetup.note".i18n())
    }
}

@Composable
private fun AccountsLoadedState(
    uiState: PaymentAccountsMusigUiState,
    paddingValues: PaddingValues,
    onAction: (PaymentAccountsMusigUiAction) -> Unit,
) {
    val tabItems = PaymentAccountTab.entries.map { it to it.titleKey.i18n() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        BisqSegmentButton(
            value = uiState.selectedTab,
            items = tabItems,
            onValueChange = { onAction(PaymentAccountsMusigUiAction.OnTabSelect(it.first)) },
        )

        Box(
            modifier =
                Modifier
                    .padding(top = 16.dp)
                    .weight(1f),
        ) {
            when (uiState.selectedTab) {
                PaymentAccountTab.FIAT -> {
                    if (uiState.fiatAccounts.isEmpty()) {
                        EmptyAccountsInfoSection()
                    } else {
                        FiatAccountsList(uiState.fiatAccounts)
                    }
                }

                PaymentAccountTab.CRYPTO -> {
                    if (uiState.cryptoAccounts.isEmpty()) {
                        EmptyAccountsInfoSection()
                    } else {
                        CryptoAccountsList(uiState.cryptoAccounts)
                    }
                }
            }
        }

        BisqButton(
            text = if (uiState.selectedTab == PaymentAccountTab.FIAT) "paymentAccounts.createAccount".i18n() else "paymentAccounts.crypto.createAccount".i18n(),
            onClick = { onAction(if (uiState.selectedTab == PaymentAccountTab.FIAT) PaymentAccountsMusigUiAction.OnAddFiatAccountClick else PaymentAccountsMusigUiAction.OnAddCryptoAccountClick) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
        )
    }
}

@Composable
fun FiatAccountsList(accounts: List<FiatAccountVO>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(accounts, key = { it.accountName }) { account ->
            FiatPaymentAccountCard(account)
        }
    }
}

@Composable
fun CryptoAccountsList(accounts: List<CryptoAccountVO>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(accounts, key = { it.accountName }) { account ->
            CryptoPaymentAccountCard(account)
        }
    }
}

@ExcludeFromCoverage
@Composable
private fun PreviewTopBar() {
    TopBarContent(
        title = "paymentAccounts.headline".i18n(),
        showBackButton = true,
        showUserAvatar = true,
    )
}

private val previewOnAction: (PaymentAccountsMusigUiAction) -> Unit = {}

private val previewFiatAccounts =
    listOf(
        FiatAccountVO(
            accountName = "Zelle - Personal",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.LOW,
            paymentMethod = PaymentMethodVO.ZELLE,
            paymentMethodName = "Zelle",
            country = "United States",
            currency = "USD (US Dollar)",
        ),
        FiatAccountVO(
            accountName = "SEPA - Household",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.VERY_LOW,
            paymentMethod = PaymentMethodVO.SEPA,
            paymentMethodName = "Sepa",
            country = "Germany",
            currency = "EUR (Euro)",
        ),
        FiatAccountVO(
            accountName = "Wise Main",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.MODERATE,
            paymentMethod = PaymentMethodVO.WISE,
            paymentMethodName = "Wise",
            country = "United Kingdom",
            currency = "GBP (Pound Sterling)",
        ),
    )

private val previewCryptoAccounts =
    listOf(
        CryptoAccountVO(
            accountName = "Monero Main",
            currencyName = "Monero",
            address = "84ABcdXy12pqRstUvw3456EfGh7890JKLMnOPQ",
            paymentMethod = PaymentMethodVO.XMR,
        ),
        CryptoAccountVO(
            accountName = "Ethereum Wallet",
            currencyName = "Ethereum",
            address = "0x1fA2b3C4d5E6f708901234567890AbCdEf123456",
            paymentMethod = PaymentMethodVO.ETH,
        ),
        CryptoAccountVO(
            accountName = "BSQ Savings",
            currencyName = "Bisq DAO",
            address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
            paymentMethod = PaymentMethodVO.BSQ,
        ),
    )

@Preview
@Composable
private fun PaymentAccountsScreen_EmptyPreview() {
    BisqTheme.Preview {
        PaymentAccountsMusigContent(
            uiState = PaymentAccountsMusigUiState(),
            onAction = previewOnAction,
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_FiatWithAccountsPreview() {
    BisqTheme.Preview {
        PaymentAccountsMusigContent(
            uiState =
                PaymentAccountsMusigUiState(
                    fiatAccounts = previewFiatAccounts,
                    selectedTab = PaymentAccountTab.FIAT,
                ),
            onAction = previewOnAction,
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_FiatWithDeleteDialogPreview() {
    BisqTheme.Preview {
        PaymentAccountsMusigContent(
            uiState =
                PaymentAccountsMusigUiState(
                    fiatAccounts = previewFiatAccounts,
                    selectedTab = PaymentAccountTab.FIAT,
                    showDeleteConfirmationDialog = true,
                ),
            onAction = previewOnAction,
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_CryptoEmptyPreview() {
    BisqTheme.Preview {
        PaymentAccountsMusigContent(
            uiState =
                PaymentAccountsMusigUiState(
                    selectedTab = PaymentAccountTab.CRYPTO,
                    cryptoAccounts = emptyList(),
                ),
            onAction = previewOnAction,
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_CryptoWithAccountsPreview() {
    BisqTheme.Preview {
        PaymentAccountsMusigContent(
            uiState =
                PaymentAccountsMusigUiState(
                    selectedTab = PaymentAccountTab.CRYPTO,
                    cryptoAccounts = previewCryptoAccounts,
                ),
            onAction = previewOnAction,
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_LoadingPreview() {
    BisqTheme.Preview {
        PaymentAccountsMusigContent(
            uiState = PaymentAccountsMusigUiState(isLoadingAccounts = true),
            onAction = previewOnAction,
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_ErrorPreview() {
    BisqTheme.Preview {
        PaymentAccountsMusigContent(
            uiState = PaymentAccountsMusigUiState(isLoadingAccountsError = true),
            onAction = previewOnAction,
            topBar = { PreviewTopBar() },
        )
    }
}
