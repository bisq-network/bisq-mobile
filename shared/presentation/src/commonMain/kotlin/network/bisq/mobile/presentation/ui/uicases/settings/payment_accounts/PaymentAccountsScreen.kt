package network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.ErrorState
import network.bisq.mobile.presentation.ui.components.LoadingState
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqDropdown
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.organisms.BisqSnackbar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnAccountDescriptionChange
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnAccountNameChange
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnAccountSelect
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnAddAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnCancelAddAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnCancelDeleteAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnCancelEditAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnConfirmAddAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnConfirmDeleteAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnDeleteAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnEditAccountClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnRetryLoadAccountsClick
import network.bisq.mobile.presentation.ui.uicases.settings.payment_accounts.PaymentAccountsUiAction.OnSaveAccountClick
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun PaymentAccountsScreen() {
    val presenter: PaymentAccountsPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    PaymentAccountsContent(
        uiState = uiState,
        onAction = presenter::onAction,
        snackbarHostState = presenter.getSnackState(),
        topBar = { TopBar("paymentAccounts.headline".i18n()) }
    )
}

@Composable
fun PaymentAccountsContent(
    uiState: PaymentAccountsUiState,
    onAction: (PaymentAccountsUiAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    topBar: @Composable () -> Unit = {}
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = topBar,
        containerColor = BisqTheme.colors.backgroundColor,
        snackbarHost = { BisqSnackbar(snackbarHostState = snackbarHostState) }
    ) { paddingValues ->

        if (uiState.showAddAccountBottomSheet) {
            BisqBottomSheet(
                containerColor = BisqTheme.colors.dark_grey20,
                onDismissRequest = { onAction(OnCancelAddAccountClick) }
            ) {
                AppPaymentAccountCard(
                    onCancel = { onAction(OnCancelAddAccountClick) },
                    onConfirm = { name, description ->
                        onAction(OnConfirmAddAccountClick(name, description))
                    },
                )
            }
        }

        when {
            uiState.isLoadingAccounts -> {
                LoadingState(paddingValues)
            }

            uiState.isLoadingAccountsError -> {
                ErrorState(
                    paddingValues = paddingValues,
                    onRetry = { onAction(OnRetryLoadAccountsClick) }
                )
            }

            uiState.accounts.isEmpty() -> {
                EmptyAccountsState(
                    paddingValues = paddingValues,
                    onAction = onAction
                )
            }

            uiState.accounts.isNotEmpty() -> {
                AccountsListState(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onAction = onAction
                )
            }
        }

        if (uiState.showDeleteConfirmationDialog) {
            ConfirmationDialog(
                onConfirm = { onAction(OnConfirmDeleteAccountClick) },
                onDismiss = { onAction(OnCancelDeleteAccountClick) }
            )
        }
    }
}

@Composable
private fun EmptyAccountsState(
    paddingValues: PaddingValues,
    onAction: (PaymentAccountsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            BisqText.h4LightGrey("paymentAccounts.noAccounts.info".i18n())
            BisqGap.V2()
            BisqText.h2Light("paymentAccounts.noAccounts.whySetup".i18n())
            BisqGap.V1()
            BisqText.baseLight("paymentAccounts.noAccounts.whySetup.info".i18n())
            BisqGap.V2()
            BisqText.baseLightGrey("paymentAccounts.noAccounts.whySetup.note".i18n())
        }

        BisqButton(
            text = "paymentAccounts.createAccount".i18n(),
            onClick = { onAction(OnAddAccountClick) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
    }
}

@Composable
private fun AccountsListState(
    uiState: PaymentAccountsUiState,
    paddingValues: PaddingValues,
    onAction: (PaymentAccountsUiAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {

        Column {
            if (uiState.isEditAccountMode) {
                BisqTextFieldV0(
                    value = uiState.accountName,
                    onValueChange = { onAction(OnAccountNameChange(it)) },
                    isError = uiState.accountNameInvalidMessage != null,
                    bottomMessage = uiState.accountNameInvalidMessage,
                    label = "mobile.user.paymentAccounts.createAccount.paymentAccount.label".i18n()
                )
            } else {
                BisqDropdown(
                    options = uiState.accounts.map { it.accountName },
                    selectedIndex = uiState.selectedAccountIndex,
                    onOptionSelected = { onAction(OnAccountSelect(it)) },
                    label = "mobile.user.paymentAccounts.createAccount.paymentAccount.label".i18n()
                )
            }

            BisqGap.V1()

            BisqTextFieldV0(
                value = uiState.accountDescription,
                onValueChange = { onAction(OnAccountDescriptionChange(it)) },
                label = "paymentAccounts.legacy.accountData".i18n(),
                enabled = uiState.isEditAccountMode,
                isError = uiState.accountDescriptionInvalidMessage != null,
                bottomMessage = uiState.accountDescriptionInvalidMessage,
                minLines = 4,
            )

            BisqGap.V1()

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isEditAccountMode) {
                    BisqButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        text = "action.save".i18n(),
                        onClick = { onAction(OnSaveAccountClick) },
                    )
                    BisqButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        text = "paymentAccounts.deleteAccount".i18n(),
                        type = BisqButtonType.Grey,
                        onClick = { onAction(OnDeleteAccountClick) },
                    )
                    BisqButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        text = "action.cancel".i18n(),
                        type = BisqButtonType.Outline,
                        onClick = { onAction(OnCancelEditAccountClick) },
                    )
                } else {
                    BisqButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        text = "action.edit".i18n(),
                        onClick = { onAction(OnEditAccountClick) },
                    )
                }
            }
        }

        if (!uiState.isEditAccountMode) {
            BisqButton(
                text = "paymentAccounts.legacy.createAccount.headline".i18n(),
                onClick = { onAction(OnAddAccountClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun PreviewTopBar() {
    TopBarContent(
        title = "paymentAccounts.headline".i18n(),
        showBackButton = true,
        showUserAvatar = true
    )
}

@Composable
private fun previewSnackbarHostState() = remember { SnackbarHostState() }

private val previewOnAction: (PaymentAccountsUiAction) -> Unit = {}

@Preview
@Composable
private fun PaymentAccountsScreenPreview_Empty() {
    BisqTheme.Preview {
        PaymentAccountsContent(
            uiState = PaymentAccountsUiState(),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() }
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreenPreview_WithAccounts() {
    val sampleAccounts = listOf(
        UserDefinedFiatAccountVO(
            accountName = "PayPal Account",
            accountPayload = UserDefinedFiatAccountPayloadVO(
                accountData = "user@example.com"
            )
        ),
        UserDefinedFiatAccountVO(
            accountName = "Bank Transfer",
            accountPayload = UserDefinedFiatAccountPayloadVO(
                accountData = "IBAN: DE89370400440532013000"
            )
        ),
        UserDefinedFiatAccountVO(
            accountName = "Revolut",
            accountPayload = UserDefinedFiatAccountPayloadVO(
                accountData = "+1234567890"
            )
        )
    )

    BisqTheme.Preview {
        PaymentAccountsContent(
            uiState = PaymentAccountsUiState(
                accounts = sampleAccounts,
                selectedAccountIndex = 0,
                accountName = sampleAccounts[0].accountName,
                accountDescription = sampleAccounts[0].accountPayload.accountData,
            ),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() }
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreenPreview_EditMode() {
    val sampleAccounts = listOf(
        UserDefinedFiatAccountVO(
            accountName = "PayPal Account",
            accountPayload = UserDefinedFiatAccountPayloadVO(
                accountData = "user@example.com"
            )
        ),
        UserDefinedFiatAccountVO(
            accountName = "Bank Transfer",
            accountPayload = UserDefinedFiatAccountPayloadVO(
                accountData = "IBAN: DE89370400440532013000"
            )
        ),
        UserDefinedFiatAccountVO(
            accountName = "Revolut",
            accountPayload = UserDefinedFiatAccountPayloadVO(
                accountData = "+1234567890"
            )
        )
    )

    BisqTheme.Preview {
        PaymentAccountsContent(
            uiState = PaymentAccountsUiState(
                accounts = sampleAccounts,
                selectedAccountIndex = 1,
                accountName = sampleAccounts[1].accountName,
                accountDescription = sampleAccounts[1].accountPayload.accountData,
                isEditAccountMode = true
            ),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() }
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreenPreview_Loading() {
    BisqTheme.Preview {
        PaymentAccountsContent(
            uiState = PaymentAccountsUiState(isLoadingAccounts = true),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() }
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreenPreview_Error() {
    BisqTheme.Preview {
        PaymentAccountsContent(
            uiState = PaymentAccountsUiState(isLoadingAccountsError = true),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() }
        )
    }
}
