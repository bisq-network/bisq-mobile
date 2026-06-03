package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.ach_transfer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.presentation.common.util.toDisplayString
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqDropdown
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
@Composable
fun AchTransferFormContent(
    presenter: AchTransferFormPresenter,
    onNavigateToNextScreen: (CreatePaymentAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by presenter.uiState.collectAsState()
    val currentOnNavigate by rememberUpdatedState(onNavigateToNextScreen)

    LaunchedEffect(presenter) {
        presenter.effect.collect { effect ->
            when (effect) {
                is AchTransferFormEffect.NavigateToNextScreen -> currentOnNavigate(effect.account)
            }
        }
    }

    AchTransferFormContent(
        uiState = uiState,
        onAction = presenter::onAction,
        modifier = modifier,
    )
}

@Composable
private fun AchTransferFormContent(
    uiState: AchTransferFormUiState,
    onAction: (AchTransferFormUiAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        val holderNameLabel = "paymentAccounts.holderName".i18n()
        AchTransferTextField(
            entry = uiState.holderNameEntry,
            label = holderNameLabel,
            placeholder = "paymentAccounts.createAccount.prompt".i18n(holderNameLabel.lowercase()),
            onValueChange = { onAction(AchTransferFormUiAction.OnHolderNameChange(it)) },
        )

        val holderAddressLabel = "paymentAccounts.holderAddress".i18n()
        AchTransferTextField(
            entry = uiState.holderAddressEntry,
            label = holderAddressLabel,
            placeholder = "paymentAccounts.createAccount.prompt".i18n(holderAddressLabel.lowercase()),
            onValueChange = { onAction(AchTransferFormUiAction.OnHolderAddressChange(it)) },
        )

        val bankNameLabel = "paymentAccounts.bank.bankName".i18n()
        AchTransferTextField(
            entry = uiState.bankNameEntry,
            label = bankNameLabel,
            placeholder = "paymentAccounts.createAccount.prompt".i18n(bankNameLabel.lowercase()),
            onValueChange = { onAction(AchTransferFormUiAction.OnBankNameChange(it)) },
        )

        val routingNrLabel = "paymentAccounts.achTransfer.routingNr".i18n()
        AchTransferTextField(
            entry = uiState.routingNrEntry,
            label = routingNrLabel,
            placeholder = "paymentAccounts.createAccount.prompt".i18n(routingNrLabel.lowercase()),
            onValueChange = { onAction(AchTransferFormUiAction.OnRoutingNrChange(it)) },
        )

        val accountNrLabel = "paymentAccounts.accountNr".i18n()
        AchTransferTextField(
            entry = uiState.accountNrEntry,
            label = accountNrLabel,
            placeholder = "paymentAccounts.createAccount.prompt".i18n(accountNrLabel.lowercase()),
            onValueChange = { onAction(AchTransferFormUiAction.OnAccountNrChange(it)) },
        )

        AchTransferBankAccountTypeDropdown(
            selectedBankAccountType = uiState.selectedBankAccountType,
            errorMessage = uiState.bankAccountTypeErrorMessage,
            onOptionSelect = { type -> onAction(AchTransferFormUiAction.OnBankAccountTypeSelect(type)) },
        )
    }
}

@Composable
private fun AchTransferTextField(
    entry: DataEntry,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
) {
    BisqTextFieldV0(
        modifier = modifier.padding(top = 12.dp),
        value = entry.value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        isError = entry.errorMessage != null,
        bottomMessage = entry.errorMessage,
        singleLine = true,
    )
}

@Composable
private fun AchTransferBankAccountTypeDropdown(
    selectedBankAccountType: BankAccountType?,
    errorMessage: String?,
    onOptionSelect: (BankAccountType) -> Unit,
) {
    BisqGap.V1()
    BisqDropdown(
        options = BankAccountType.entries.map { type -> type.toDisplayString() },
        selectedIndex = BankAccountType.entries.indexOf(selectedBankAccountType),
        onOptionSelect = { index ->
            BankAccountType.entries.getOrNull(index)?.let(onOptionSelect)
        },
        modifier = Modifier.fillMaxWidth(),
        label = "paymentAccounts.bank.bankAccountType".i18n(),
        prompt = "paymentAccounts.createAccount.accountData.bank.bankAccountType.prompt".i18n(),
    )

    errorMessage?.let { message ->
        BisqGap.VHalf()
        BisqText.SmallLight(
            text = message,
            color = BisqTheme.colors.danger,
        )
    }
}

@Preview
@Composable
private fun AchTransferFormContentPreview() {
    BisqTheme.Preview {
        AchTransferFormContent(
            uiState =
                AchTransferFormUiState(
                    holderNameEntry = DataEntry(value = "Alice Doe"),
                    holderAddressEntry = DataEntry(value = "123 Main St"),
                    bankNameEntry = DataEntry(value = "Bisq Bank"),
                    routingNrEntry = DataEntry(value = "123456789"),
                    accountNrEntry = DataEntry(value = "000123456789"),
                    selectedBankAccountType = BankAccountType.CHECKING,
                ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun AchTransferFormContentErrorPreview() {
    BisqTheme.Preview {
        AchTransferFormContent(
            uiState =
                AchTransferFormUiState(
                    holderNameEntry = DataEntry(value = "A", errorMessage = "validation.tooShortOrTooLong".i18n(2, 70)),
                    bankAccountTypeErrorMessage = "paymentAccounts.createAccount.accountData.bank.bankAccountType.error.noneSelected".i18n(),
                ),
            onAction = {},
        )
    }
}
