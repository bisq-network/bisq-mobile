package network.bisq.mobile.presentation.create_payment_account.select_payment_method.crypto

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
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.PaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.ui.CryptoPaymentMethodCard

@ExcludeFromCoverage
@Composable
fun SelectCryptoPaymentMethodScreen(
    onNavigateToNextScreen: (PaymentMethodVO) -> Unit,
) {
    val presenter = RememberPresenterLifecycleBackStackAware<SelectCryptoPaymentMethodPresenter>()

    LaunchedEffect(presenter, onNavigateToNextScreen) {
        presenter.effect.collect { effect ->
            when (effect) {
                is SelectCryptoPaymentMethodEffect.NavigateToNextScreen -> onNavigateToNextScreen(effect.selectedPaymentMethod)
            }
        }
    }

    val uiState by presenter.uiState.collectAsState()

    SelectCryptoPaymentMethodContent(
        uiState = uiState,
        onAction = presenter::onAction,
    )
}

@Composable
fun SelectCryptoPaymentMethodContent(
    uiState: SelectCryptoPaymentMethodUiState,
    onAction: (SelectCryptoPaymentMethodUiAction) -> Unit,
) {
    when {
        uiState.isLoading -> {
            LoadingState()
        }

        uiState.isError -> {
            ErrorState(
                onRetry = { onAction(SelectCryptoPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick) },
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
    uiState: SelectCryptoPaymentMethodUiState,
    onAction: (SelectCryptoPaymentMethodUiAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        BisqText.H6Regular("mobile.user.paymentAccounts.crypto.select".i18n())
        BisqGap.V1()

        BisqSearchField(
            value = uiState.searchQuery,
            onValueChange = { value -> onAction(SelectCryptoPaymentMethodUiAction.OnSearchQueryChange(value)) },
        )
        BisqGap.VHalf()

        CryptoPaymentMethodsSection(
            uiState = uiState,
            onPaymentMethodClick = { onAction(SelectCryptoPaymentMethodUiAction.OnPaymentMethodClick(it)) },
        )

        BisqGap.VHalfQuarter()
        BisqButton(
            text = "action.next".i18n(),
            onClick = { onAction(SelectCryptoPaymentMethodUiAction.OnNextClick) },
            disabled = uiState.selectedPaymentMethod == null,
            modifier = Modifier.fillMaxWidth(),
        )
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

private val previewCryptoPaymentMethods =
    listOf(
        CryptoPaymentMethodVO(
            paymentType = PaymentTypeVO.XMR,
            code = "XMR",
            name = "Monero",
            supportAutoConf = false,
        ),
        CryptoPaymentMethodVO(
            paymentType = PaymentTypeVO.LNBTC,
            code = "LN-BTC",
            name = "Lightning Bitcoin",
            supportAutoConf = false,
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
        SelectCryptoPaymentMethodContent(
            uiState = SelectCryptoPaymentMethodUiState(isLoading = true),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_ErrorPreview() {
    BisqTheme.Preview {
        SelectCryptoPaymentMethodContent(
            uiState = SelectCryptoPaymentMethodUiState(isError = true),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_CryptoLoadedPreview() {
    BisqTheme.Preview {
        SelectCryptoPaymentMethodContent(
            uiState =
                SelectCryptoPaymentMethodUiState(
                    paymentMethods = previewCryptoPaymentMethods,
                    searchQuery = "btc",
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SelectPaymentMethodContentPreview_CryptoEmptyLoadedPreview() {
    BisqTheme.Preview {
        SelectCryptoPaymentMethodContent(
            uiState = SelectCryptoPaymentMethodUiState(),
            onAction = {},
        )
    }
}
