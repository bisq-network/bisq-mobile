package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqEditableDropDown
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.organisms.settings.AppPaymentAccountCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IPaymentAccountSettingsPresenter : ViewPresenter {
    val accounts: StateFlow<List<UserDefinedFiatAccountVO>>
    val selectedAccount: StateFlow<UserDefinedFiatAccountVO?>

    fun selectAccount(account: UserDefinedFiatAccountVO)

    fun addAccount(newName: String, newDescription: String)
    fun saveAccount(newName: String, newDescription: String)
    fun deleteCurrentAccount()
}

private const val MAX_ACCOUNT_FIELD_LENGTH = 1024

@Composable
fun PaymentAccountSettingsScreen() {
    val presenter: IPaymentAccountSettingsPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val accounts by presenter.accounts.collectAsState()
    val selectedAccount by presenter.selectedAccount.collectAsState()

    var accountName by remember { mutableStateOf(selectedAccount?.accountName ?: "") }
    var accountNameValid by remember { mutableStateOf(true) }
    var accountDescription by remember { mutableStateOf(selectedAccount?.accountPayload?.accountData ?: "") }
    var accountDescriptionValid by remember { mutableStateOf(true) }

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(selectedAccount) {
        accountName = selectedAccount?.accountName ?: ""
        accountDescription = selectedAccount?.accountPayload?.accountData ?: ""
        accountNameValid = true
        accountDescriptionValid = true
    }

    BisqScrollScaffold(
        topBar = { TopBar("user.paymentAccounts".i18n()) },
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        snackbarHostState = presenter.getSnackState(),
        isInteractive = isInteractive,
        shouldBlurBg = showConfirmationDialog,
    ) {
        if (showBottomSheet) {
            BisqBottomSheet(
                onDismissRequest = { showBottomSheet = false }
            ) {
                AppPaymentAccountCard(
                    onCancel = { showBottomSheet = false },
                    onConfirm = { name, description ->
                        presenter.addAccount(name, description)
                        showBottomSheet = false
                    },
                )
            }
        }

        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Top,
            ) {

                BisqText.baseLightGrey("user.paymentAccounts.noAccounts.info".i18n())
                BisqGap.V2()
                BisqText.h2Light("user.paymentAccounts.noAccounts.whySetup".i18n())
                BisqGap.V1()
                BisqText.baseLight("user.paymentAccounts.noAccounts.whySetup.info".i18n())
                BisqGap.V2()
                BisqText.baseLightGrey("user.paymentAccounts.noAccounts.whySetup.note".i18n())

                BisqGap.V2()

                BisqButton(
                    text = "user.paymentAccounts.createAccount".i18n(),
                    onClick = { showBottomSheet = !showBottomSheet },
                    modifier = Modifier.padding(all = 8.dp)
                )
            }
            return@BisqScrollScaffold
        }

        BisqButton(
            text = "user.paymentAccounts.createAccount".i18n(),
            onClick = { showBottomSheet = !showBottomSheet },
            padding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
        )

        BisqGap.V1()

        BisqEditableDropDown(
            value = accountName,
            items = accounts.map { it.accountName },
            label = "mobile.user.paymentAccounts.createAccount.paymentAccount.label".i18n(),
            onValueChanged = { name, isValid ->
                var account = accounts.firstOrNull { it.accountName == name }
                if (account == null) {
                    account = accounts.firstOrNull { it.accountPayload.accountData == accountDescription }
                }
                if (account != null) {
                    presenter.selectAccount(account)
                }
                accountName = name
                accountNameValid = isValid
            },
            validation = {
                if (it.length < 3) {
                    return@BisqEditableDropDown "mobile.user.paymentAccounts.createAccount.paymentAccount.validations.minLength".i18n()
                }

                if (it.length > MAX_ACCOUNT_FIELD_LENGTH) {
                    return@BisqEditableDropDown "mobile.user.paymentAccounts.createAccount.paymentAccount.validations.maxLength".i18n()
                }

                return@BisqEditableDropDown null
            }
        )

        BisqGap.V1()

        BisqTextField(
            value = accountDescription,
            onValueChange = { value, isValid ->
                accountDescription = value
                accountDescriptionValid = isValid
            },
            label = "user.paymentAccounts.accountData".i18n(),
            isTextArea = true,
            minLines = 4,
            validation = {

                if (it.length < 3) {
                    return@BisqTextField "mobile.user.paymentAccounts.accountData.paymentAccount.validations.minLength".i18n()
                }

                if (it.length > MAX_ACCOUNT_FIELD_LENGTH) {
                    return@BisqTextField "mobile.user.paymentAccounts.accountData.paymentAccount.validations.maxLength".i18n()
                }

                return@BisqTextField null
            }
        )

        BisqGap.V1()

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            BisqButton(
                text = "mobile.user.paymentAccounts.deleteAccount".i18n(),
                type = BisqButtonType.Grey,
                onClick = { showConfirmationDialog = true },
                disabled = selectedAccount == null
            )
            BisqButton(
                text = "action.save".i18n(),
                onClick = {
                    presenter.saveAccount(accountName, accountDescription)
                },
                disabled = !accountNameValid || !accountDescriptionValid
            )
        }
    }

    if (showConfirmationDialog) {
        ConfirmationDialog(
            onConfirm = {
                presenter.deleteCurrentAccount()
                showConfirmationDialog = false
            },
            onDismiss = {
                showConfirmationDialog = false
            }
        )
    }

}