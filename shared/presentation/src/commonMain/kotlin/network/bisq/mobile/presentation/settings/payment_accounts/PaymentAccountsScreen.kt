package network.bisq.mobile.presentation.settings.payment_accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountPayloadVO
import network.bisq.mobile.domain.data.replicated.account.fiat.UserDefinedFiatAccountVO
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqDropdown
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
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
        topBar = { TopBar("paymentAccounts.headline".i18n()) },
    )
}

@Composable
fun PaymentAccountsContent(
    uiState: PaymentAccountsUiState,
    onAction: (PaymentAccountsUiAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    topBar: @Composable () -> Unit = {},
) {
    BisqScaffold(
        topBar = topBar,
        snackbarHostState = snackbarHostState,
    ) { paddingValues ->

        when {
            uiState.isLoadingAccounts -> {
                LoadingState(paddingValues)
            }

            uiState.isLoadingAccountsError -> {
                ErrorState(
                    paddingValues = paddingValues,
                    onRetry = { onAction(PaymentAccountsUiAction.OnRetryLoadAccountsClick) },
                )
            }

            uiState.showAddAccountState -> {
                CreateAccountState(
                    paddingValues = paddingValues,
                    onAction = onAction,
                    uiState = uiState,
                )
            }

            uiState.accounts.isEmpty() -> {
                EmptyAccountsState(
                    paddingValues = paddingValues,
                    onAction = onAction,
                )
            }

            uiState.accounts.isNotEmpty() -> {
                AccountsListState(
                    uiState = uiState,
                    paddingValues = paddingValues,
                    onAction = onAction,
                )
            }
        }

        if (uiState.showDeleteConfirmationDialog) {
            ConfirmationDialog(
                onConfirm = { onAction(PaymentAccountsUiAction.OnConfirmDeleteAccountClick) },
                onDismiss = { onAction(PaymentAccountsUiAction.OnCancelDeleteAccountClick) },
            )
        }
    }
}

@Composable
private fun EmptyAccountsState(
    paddingValues: PaddingValues,
    onAction: (PaymentAccountsUiAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
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

        BisqButton(
            text = "paymentAccounts.createAccount".i18n(),
            onClick = { onAction(PaymentAccountsUiAction.OnAddAccountClick) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
        )
    }
}

@Composable
private fun AccountsListState(
    uiState: PaymentAccountsUiState,
    paddingValues: PaddingValues,
    onAction: (PaymentAccountsUiAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            PaymentAccountForm(
                uiState = uiState,
                onAction = onAction,
            )
        }

        if (!uiState.showEditAccountState) {
            BisqButton(
                text = "paymentAccounts.legacy.createAccount.headline".i18n(),
                onClick = { onAction(PaymentAccountsUiAction.OnAddAccountClick) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun CreateAccountState(
    uiState: PaymentAccountsUiState,
    paddingValues: PaddingValues,
    onAction: (PaymentAccountsUiAction) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
    ) {
        PaymentAccountForm(
            uiState = uiState,
            onAction = onAction,
        )
    }
}

@Composable
@Suppress("UnusedReceiverParameter")
private fun ColumnScope.PaymentAccountForm(
    uiState: PaymentAccountsUiState,
    onAction: (PaymentAccountsUiAction) -> Unit,
) {
    if (uiState.showEditAccountState || uiState.showAddAccountState) {
        BisqTextFieldV0(
            value = uiState.accountNameEntry.value,
            onValueChange = { onAction(PaymentAccountsUiAction.OnAccountNameChange(it)) },
            isError = uiState.accountNameEntry.errorMessage != null,
            bottomMessage = uiState.accountNameEntry.errorMessage,
            label = "mobile.user.paymentAccounts.createAccount.paymentAccount.label".i18n(),
            placeholder = "paymentAccounts.legacy.createAccount.accountName.prompt".i18n(),
        )
    } else {
        BisqDropdown(
            options = uiState.accounts.map { it.accountName },
            selectedIndex = uiState.selectedAccountIndex,
            onOptionSelect = { onAction(PaymentAccountsUiAction.OnAccountSelect(it)) },
            label = "mobile.user.paymentAccounts.createAccount.paymentAccount.label".i18n(),
        )
    }

    BisqGap.V1()

    BisqTextFieldV0(
        value = uiState.accountDescriptionEntry.value,
        onValueChange = { onAction(PaymentAccountsUiAction.OnAccountDescriptionChange(it)) },
        label = "paymentAccounts.legacy.accountData".i18n(),
        enabled = uiState.showEditAccountState || uiState.showAddAccountState,
        isError = uiState.accountDescriptionEntry.errorMessage != null,
        bottomMessage = uiState.accountDescriptionEntry.errorMessage,
        minLines = 4,
        placeholder = "paymentAccounts.legacy.createAccount.accountData.prompt".i18n(),
    )

    BisqGap.V1()

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        when {
            uiState.showEditAccountState -> {
                BisqButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    text = "action.save".i18n(),
                    onClick = { onAction(PaymentAccountsUiAction.OnSaveAccountClick) },
                )
                BisqButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    text = "paymentAccounts.deleteAccount".i18n(),
                    type = BisqButtonType.Grey,
                    onClick = { onAction(PaymentAccountsUiAction.OnDeleteAccountClick) },
                )
                BisqButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    text = "action.cancel".i18n(),
                    type = BisqButtonType.Outline,
                    onClick = { onAction(PaymentAccountsUiAction.OnCancelAddEditAccountClick) },
                )
            }

            uiState.showAddAccountState -> {
                BisqButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    text = "paymentAccounts.createAccount".i18n(),
                    onClick = { onAction(PaymentAccountsUiAction.OnConfirmAddAccountClick) },
                )
                BisqButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    text = "action.cancel".i18n(),
                    type = BisqButtonType.Outline,
                    onClick = { onAction(PaymentAccountsUiAction.OnCancelAddEditAccountClick) },
                )
            }

            else -> {
                BisqButton(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    text = "action.edit".i18n(),
                    onClick = { onAction(PaymentAccountsUiAction.OnEditAccountClick) },
                )
            }
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

@ExcludeFromCoverage
@Composable
private fun previewSnackbarHostState() = remember { SnackbarHostState() }

private val previewOnAction: (PaymentAccountsUiAction) -> Unit = {}

@Preview
@Composable
private fun PaymentAccountsScreen_EmptyPreview() {
    BisqTheme.Preview {
        PaymentAccountsContent(
            uiState = PaymentAccountsUiState(),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_WithAccountsPreview() {
    BisqTheme.Preview {
        val sampleAccounts =
            listOf(
                UserDefinedFiatAccountVO(
                    accountName = "PayPal Account",
                    accountPayload =
                        UserDefinedFiatAccountPayloadVO(
                            accountData = "user@example.com",
                        ),
                ),
                UserDefinedFiatAccountVO(
                    accountName = "Bank Transfer",
                    accountPayload =
                        UserDefinedFiatAccountPayloadVO(
                            accountData = "IBAN: DE89370400440532013000",
                        ),
                ),
                UserDefinedFiatAccountVO(
                    accountName = "Revolut",
                    accountPayload =
                        UserDefinedFiatAccountPayloadVO(
                            accountData = "+1234567890",
                        ),
                ),
            )

        PaymentAccountsContent(
            uiState =
                PaymentAccountsUiState(
                    accounts = sampleAccounts,
                    selectedAccountIndex = 0,
                    accountNameEntry = DataEntry(value = sampleAccounts[0].accountName),
                    accountDescriptionEntry =
                        DataEntry(
                            value = sampleAccounts[0].accountPayload.accountData,
                        ),
                ),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_EditModePreview() {
    BisqTheme.Preview {
        val sampleAccounts =
            listOf(
                UserDefinedFiatAccountVO(
                    accountName = "PayPal Account",
                    accountPayload =
                        UserDefinedFiatAccountPayloadVO(
                            accountData = "user@example.com",
                        ),
                ),
                UserDefinedFiatAccountVO(
                    accountName = "Bank Transfer",
                    accountPayload =
                        UserDefinedFiatAccountPayloadVO(
                            accountData = "IBAN: DE89370400440532013000",
                        ),
                ),
                UserDefinedFiatAccountVO(
                    accountName = "Revolut",
                    accountPayload =
                        UserDefinedFiatAccountPayloadVO(
                            accountData = "+1234567890",
                        ),
                ),
            )

        PaymentAccountsContent(
            uiState =
                PaymentAccountsUiState(
                    accounts = sampleAccounts,
                    selectedAccountIndex = 1,
                    accountNameEntry = DataEntry(value = sampleAccounts[1].accountName),
                    accountDescriptionEntry =
                        DataEntry(
                            value = sampleAccounts[1].accountPayload.accountData,
                        ),
                    showEditAccountState = true,
                ),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_LoadingPreview() {
    BisqTheme.Preview {
        PaymentAccountsContent(
            uiState = PaymentAccountsUiState(isLoadingAccounts = true),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_ErrorPreview() {
    BisqTheme.Preview {
        PaymentAccountsContent(
            uiState = PaymentAccountsUiState(isLoadingAccountsError = true),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountsScreen_CreateModePreview() {
    BisqTheme.Preview {
        PaymentAccountsContent(
            uiState =
                PaymentAccountsUiState(
                    showAddAccountState = true,
                ),
            onAction = previewOnAction,
            snackbarHostState = previewSnackbarHostState(),
            topBar = { PreviewTopBar() },
        )
    }
}
