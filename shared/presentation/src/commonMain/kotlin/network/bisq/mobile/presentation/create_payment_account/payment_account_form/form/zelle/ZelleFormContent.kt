package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.ZelleFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.ui.PaymentMethodBackgroundInformationDialog
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO

@ExcludeFromCoverage
@Composable
fun ZellePaymentAccountFormContent(
    presenter: ZelleFormPresenter,
    paymentMethod: FiatPaymentMethodVO,
    onNavigateToNextScreen: (PaymentAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by presenter.uiState.collectAsState()
    val currentOnNavigate by rememberUpdatedState(onNavigateToNextScreen)

    LaunchedEffect(presenter, paymentMethod) {
        presenter.initialize(paymentMethod)
    }

    LaunchedEffect(presenter) {
        presenter.effect.collect { effect ->
            when (effect) {
                is ZelleFormEffect.NavigateToNextScreen -> currentOnNavigate(effect.account)
            }
        }
    }

    ZelleFormContent(
        uiState = uiState,
        modifier = modifier,
        onAction = presenter::onAction,
        onHyperlinkOpenError = {
            presenter.showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR)
        },
    )
}

@ExcludeFromCoverage
@Composable
fun ZelleFormContent(
    uiState: ZelleFormUiState,
    onAction: (ZelleFormUiAction) -> Unit,
    modifier: Modifier = Modifier,
    onHyperlinkOpenError: (() -> Unit)? = null,
) {
    val isInPreview = LocalInspectionMode.current
    val (showBackgroundInformationDialog, setShowBackgroundInformationDialog) =
        rememberSaveable { mutableStateOf(!isInPreview) }

    Column(modifier = modifier) {
        BisqTextFieldV0(
            value = uiState.holderNameEntry.value,
            onValueChange = { onAction(ZelleFormUiAction.OnHolderNameChange(it)) },
            label = "paymentAccounts.holderName".i18n(),
            placeholder =
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.holderName".i18n().lowercase(),
                ),
            isError = uiState.holderNameEntry.errorMessage != null,
            bottomMessage = uiState.holderNameEntry.errorMessage,
            singleLine = true,
        )

        BisqTextFieldV0(
            modifier = Modifier.padding(top = 12.dp),
            value = uiState.emailOrMobileNrEntry.value,
            onValueChange = { onAction(ZelleFormUiAction.OnEmailOrMobileNrChange(it)) },
            label = "paymentAccounts.emailOrMobileNr".i18n(),
            placeholder =
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.emailOrMobileNr".i18n().lowercase(),
                ),
            isError = uiState.emailOrMobileNrEntry.errorMessage != null,
            bottomMessage = uiState.emailOrMobileNrEntry.errorMessage,
            singleLine = true,
        )
    }

    if (showBackgroundInformationDialog) {
        PaymentMethodBackgroundInformationDialog(
            bodyText = "paymentAccounts.createAccount.accountData.backgroundOverlay.zelle".i18n(),
            onDismissRequest = { setShowBackgroundInformationDialog(false) },
            onError = onHyperlinkOpenError,
        )
    }
}

@Suppress("unused")
@Preview
@Composable
private fun ZelleFormContentPreview() {
    BisqTheme.Preview {
        ZelleFormContent(
            uiState =
                ZelleFormUiState(
                    holderNameEntry = DataEntry(value = "Alice Doe"),
                    emailOrMobileNrEntry = DataEntry(value = "alice@example.com"),
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun ZelleFormContentErrorPreview() {
    BisqTheme.Preview {
        ZelleFormContent(
            uiState =
                ZelleFormUiState(
                    holderNameEntry =
                        DataEntry(
                            value = "A",
                            errorMessage = "validation.holderNameInvalidLength".i18n(),
                        ),
                    emailOrMobileNrEntry =
                        DataEntry(
                            value = "not-valid",
                            errorMessage = "validation.invalidEmailOrPhoneNumber".i18n(),
                        ),
                ),
            onAction = {},
        )
    }
}
