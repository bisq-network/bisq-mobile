package network.bisq.mobile.presentation.create_payment_account.select_payment_method

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui.CryptoPaymentMethodCard
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui.FiatChargebackRiskFilterSection
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui.FiatPaymentMethodCard

@ExcludeFromCoverage
@Composable
fun SelectPaymentMethodScreen(
    accountType: PaymentAccountType,
    onContinue: (PaymentMethodVO) -> Unit = {},
) {
    val presenter = RememberPresenterLifecycleBackStackAware<SelectPaymentMethodPresenter>()
    LaunchedEffect(accountType) {
        presenter.initialize(accountType)
    }

    val uiState by presenter.uiState.collectAsState()

    SelectPaymentMethodContent(
        uiState = uiState,
        accountType = accountType,
        onAction = presenter::onAction,
        onContinue = onContinue,
    )
}

@Composable
fun SelectPaymentMethodContent(
    uiState: SelectPaymentMethodUiState,
    accountType: PaymentAccountType,
    onAction: (SelectPaymentMethodUiAction) -> Unit,
    onContinue: (PaymentMethodVO) -> Unit = {},
) {
    when {
        uiState.isLoading -> {
            LoadingState()
        }

        uiState.isError -> {
            ErrorState(
                onRetry = { onAction(SelectPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick) },
            )
        }

        else -> {
            PaymentMethodsLoadedState(
                uiState = uiState,
                accountType = accountType,
                onContinue = onContinue,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun PaymentMethodsLoadedState(
    uiState: SelectPaymentMethodUiState,
    accountType: PaymentAccountType,
    onContinue: (PaymentMethodVO) -> Unit,
    onAction: (SelectPaymentMethodUiAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        val headerText =
            when (accountType) {
                PaymentAccountType.FIAT -> "mobile.user.paymentAccounts.fiat.select".i18n()
                PaymentAccountType.CRYPTO -> "mobile.user.paymentAccounts.crypto.select".i18n()
            }
        BisqText.H6Regular(headerText)
        BisqGap.V1()

        BisqSearchField(
            value = uiState.searchQuery,
            onValueChange = { value -> onAction(SelectPaymentMethodUiAction.OnSearchQueryChange(value)) },
        )
        BisqGap.VHalf()

        when (accountType) {
            PaymentAccountType.FIAT -> {
                FiatPaymentMethodsSection(
                    uiState = uiState,
                    onRiskFilterChange = { onAction(SelectPaymentMethodUiAction.OnRiskFilterChange(it)) },
                    onPaymentMethodClick = { onAction(SelectPaymentMethodUiAction.OnFiatPaymentMethodClick(it)) },
                )
            }

            PaymentAccountType.CRYPTO -> {
                CryptoPaymentMethodsSection(
                    uiState = uiState,
                    onPaymentMethodClick = { onAction(SelectPaymentMethodUiAction.OnCryptoPaymentMethodClick(it)) },
                )
            }
        }

        val selectedPaymentMethod =
            when (accountType) {
                PaymentAccountType.FIAT -> uiState.selectedFiatPaymentMethod?.paymentMethod
                PaymentAccountType.CRYPTO -> uiState.selectedCryptoPaymentMethod?.paymentMethod
            }
        val isNextDisabled = selectedPaymentMethod == null

        BisqGap.VHalfQuarter()
        BisqButton(
            text = "action.next".i18n(),
            onClick = { selectedPaymentMethod?.let(onContinue) },
            disabled = isNextDisabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColumnScope.FiatPaymentMethodsSection(
    uiState: SelectPaymentMethodUiState,
    onRiskFilterChange: (FiatPaymentMethodChargebackRiskVO?) -> Unit,
    onPaymentMethodClick: (FiatPaymentMethodVO) -> Unit,
) {
    BisqText.BaseLightGrey(
        modifier = Modifier.padding(vertical = 6.dp),
        text = "paymentAccounts.createAccount.paymentMethod.table.chargebackRisk".i18n(),
    )

    FiatChargebackRiskFilterSection(
        activeRiskFilter = uiState.activeRiskFilter,
        onRiskFilterChange = onRiskFilterChange,
    )
    BisqGap.VHalfQuarter()

    if (uiState.fiatPaymentMethods.isEmpty()) {
        EmptyPaymentMethodsState(
            modifier = Modifier.weight(1f),
            text = "mobile.components.select.empty".i18n(),
        )
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.fiatPaymentMethods, key = { it.paymentMethod }) { account ->
                FiatPaymentMethodCard(
                    paymentMethod = account,
                    isSelected = uiState.selectedFiatPaymentMethod == account,
                    onClick = { onPaymentMethodClick(account) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CryptoPaymentMethodsSection(
    uiState: SelectPaymentMethodUiState,
    onPaymentMethodClick: (CryptoPaymentMethodVO) -> Unit,
) {
    BisqGap.VHalfQuarter()

    if (uiState.cryptoPaymentMethods.isEmpty()) {
        EmptyPaymentMethodsState(
            modifier = Modifier.weight(1f),
            text = "mobile.components.select.empty".i18n(),
        )
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.cryptoPaymentMethods, key = { it.paymentMethod }) { account ->
                CryptoPaymentMethodCard(
                    paymentMethod = account,
                    isSelected = uiState.selectedCryptoPaymentMethod == account,
                    onClick = { onPaymentMethodClick(account) },
                )
            }
        }
    }
}

private val previewFiatPaymentMethods =
    listOf(
        FiatPaymentMethodVO(
            paymentMethod = PaymentMethodVO.SEPA,
            name = "SEPA",
            supportedCurrencyCodes = "EUR, USD, GBP",
            countryNames = "Germany, France, Netherlands",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.VERY_LOW,
        ),
        FiatPaymentMethodVO(
            paymentMethod = PaymentMethodVO.ZELLE,
            name = "Zelle",
            supportedCurrencyCodes = "USD",
            countryNames = "United States",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.LOW,
        ),
    )

private val previewCryptoPaymentMethods =
    listOf(
        CryptoPaymentMethodVO(
            paymentMethod = PaymentMethodVO.XMR,
            code = "XMR",
            name = "Monero",
        ),
        CryptoPaymentMethodVO(
            paymentMethod = PaymentMethodVO.LNBTC,
            code = "LN-BTC",
            name = "Lightning Bitcoin",
        ),
    )

@Composable
private fun EmptyPaymentMethodsState(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.BaseRegular(
            text = text,
            textAlign = TextAlign.Center,
            color = BisqTheme.colors.mid_grey20,
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_LoadingPreview() {
    BisqTheme.Preview {
        SelectPaymentMethodContent(
            uiState = SelectPaymentMethodUiState(isLoading = true),
            accountType = PaymentAccountType.FIAT,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_ErrorPreview() {
    BisqTheme.Preview {
        SelectPaymentMethodContent(
            uiState = SelectPaymentMethodUiState(isError = true),
            accountType = PaymentAccountType.FIAT,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_FiatLoadedPreview() {
    BisqTheme.Preview {
        SelectPaymentMethodContent(
            uiState =
                SelectPaymentMethodUiState(
                    fiatPaymentMethods = previewFiatPaymentMethods,
                    searchQuery = "se",
                ),
            accountType = PaymentAccountType.FIAT,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_CryptoLoadedPreview() {
    BisqTheme.Preview {
        SelectPaymentMethodContent(
            uiState =
                SelectPaymentMethodUiState(
                    cryptoPaymentMethods = previewCryptoPaymentMethods,
                    searchQuery = "btc",
                ),
            accountType = PaymentAccountType.CRYPTO,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_FiatEmptyLoadedPreview() {
    BisqTheme.Preview {
        SelectPaymentMethodContent(
            uiState = SelectPaymentMethodUiState(),
            accountType = PaymentAccountType.FIAT,
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_CryptoEmptyLoadedPreview() {
    BisqTheme.Preview {
        SelectPaymentMethodContent(
            uiState = SelectPaymentMethodUiState(),
            accountType = PaymentAccountType.CRYPTO,
            onAction = {},
        )
    }
}
