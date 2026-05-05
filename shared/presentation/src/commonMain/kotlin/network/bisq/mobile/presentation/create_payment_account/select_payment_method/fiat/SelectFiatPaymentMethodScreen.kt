package network.bisq.mobile.presentation.create_payment_account.select_payment_method.fiat

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
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.crypto.SelectCryptoPaymentMethodUiState
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.PaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui.CryptoPaymentMethodCard
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui.FiatChargebackRiskFilterSection
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui.FiatPaymentMethodCard

@ExcludeFromCoverage
@Composable
fun SelectFiatPaymentMethodScreen(
    onNavigateToNextScreen: (PaymentMethodVO) -> Unit,
) {
    val presenter = RememberPresenterLifecycleBackStackAware<SelectFiatPaymentMethodPresenter>()

    LaunchedEffect(presenter, onNavigateToNextScreen) {
        presenter.effect.collect { effect ->
            when (effect) {
                is SelectFiatPaymentMethodEffect.NavigateToNextScreen -> onNavigateToNextScreen(effect.selectedPaymentMethod)
            }
        }
    }

    val uiState by presenter.uiState.collectAsState()

    SelectFiatPaymentMethodContent(
        uiState = uiState,
        onAction = presenter::onAction,
    )
}

@Composable
fun SelectFiatPaymentMethodContent(
    uiState: SelectFiatPaymentMethodUiState,
    onAction: (SelectFiatPaymentMethodUiAction) -> Unit,
) {
    when {
        uiState.isLoading -> {
            LoadingState()
        }

        uiState.isError -> {
            ErrorState(
                onRetry = { onAction(SelectFiatPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick) },
            )
        }

        else -> {
            PaymentMethodsLoadedState(
                uiState = uiState,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun PaymentMethodsLoadedState(
    uiState: SelectFiatPaymentMethodUiState,
    onAction: (SelectFiatPaymentMethodUiAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        BisqText.H6Regular("mobile.user.paymentAccounts.fiat.select".i18n())
        BisqGap.V1()

        BisqSearchField(
            value = uiState.searchQuery,
            onValueChange = { value -> onAction(SelectFiatPaymentMethodUiAction.OnSearchQueryChange(value)) },
        )
        BisqGap.VHalf()

        FiatPaymentMethodsSection(
            uiState = uiState,
            onRiskFilterChange = { onAction(SelectFiatPaymentMethodUiAction.OnRiskFilterChange(it)) },
            onPaymentMethodClick = { onAction(SelectFiatPaymentMethodUiAction.OnPaymentMethodClick(it)) },
        )

        BisqGap.VHalfQuarter()
        BisqButton(
            text = "action.next".i18n(),
            onClick = { onAction(SelectFiatPaymentMethodUiAction.OnNextClick) },
            disabled = uiState.selectedPaymentMethod == null,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ColumnScope.FiatPaymentMethodsSection(
    uiState: SelectFiatPaymentMethodUiState,
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

    if (uiState.paymentMethods.isEmpty()) {
        EmptyPaymentMethodsState(
            modifier = Modifier.weight(1f),
            text = "mobile.components.select.empty".i18n(),
        )
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.paymentMethods, key = { it.paymentType }) { account ->
                FiatPaymentMethodCard(
                    paymentMethod = account,
                    isSelected = uiState.selectedPaymentMethod == account,
                    onClick = { onPaymentMethodClick(account) },
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CryptoPaymentMethodsSection(
    uiState: SelectCryptoPaymentMethodUiState,
    onPaymentMethodClick: (CryptoPaymentMethodVO) -> Unit,
) {
    BisqGap.VHalfQuarter()

    if (uiState.paymentMethods.isEmpty()) {
        EmptyPaymentMethodsState(
            modifier = Modifier.weight(1f),
            text = "mobile.components.select.empty".i18n(),
        )
    } else {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(uiState.paymentMethods, key = { it.paymentType }) { account ->
                CryptoPaymentMethodCard(
                    paymentMethod = account,
                    isSelected = uiState.selectedPaymentMethod == account,
                    onClick = { onPaymentMethodClick(account) },
                )
            }
        }
    }
}

private val previewFiatPaymentMethods =
    listOf(
        FiatPaymentMethodVO(
            paymentType = PaymentTypeVO.SEPA,
            name = "SEPA",
            supportedCurrencyCodes = "EUR, USD, GBP",
            countryNames = "Germany, France, Netherlands",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.VERY_LOW,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
        ),
        FiatPaymentMethodVO(
            paymentType = PaymentTypeVO.ZELLE,
            name = "Zelle",
            supportedCurrencyCodes = "USD",
            countryNames = "United States",
            chargebackRisk = FiatPaymentMethodChargebackRiskVO.LOW,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
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
        SelectFiatPaymentMethodContent(
            uiState = SelectFiatPaymentMethodUiState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_ErrorPreview() {
    BisqTheme.Preview {
        SelectFiatPaymentMethodContent(
            uiState = SelectFiatPaymentMethodUiState(isError = true),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_FiatLoadedPreview() {
    BisqTheme.Preview {
        SelectFiatPaymentMethodContent(
            uiState =
                SelectFiatPaymentMethodUiState(
                    paymentMethods = previewFiatPaymentMethods,
                    searchQuery = "se",
                ),
            onAction = {},
        )
    }
}
