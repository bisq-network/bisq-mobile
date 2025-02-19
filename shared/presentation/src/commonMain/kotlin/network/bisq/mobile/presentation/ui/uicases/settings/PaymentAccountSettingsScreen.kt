package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqEditableDropDown
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.molecules.BisqBottomSheet
import network.bisq.mobile.presentation.ui.components.molecules.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.organisms.settings.AppPaymentAccountCard
import network.bisq.mobile.presentation.ui.composeModels.PaymentAccount
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IPaymentAccountSettingsPresenter : ViewPresenter {
    val accounts: StateFlow<List<PaymentAccount>>
    val selectedAccount: StateFlow<PaymentAccount>

    fun selectAccount(account: PaymentAccount)

    fun addAccount(newName: String, newDescription: String)
    fun saveAccount(newName: String, newDescription: String)
    fun deleteCurrentAccount()
}

// TODO: Toast messages
@Composable
fun PaymentAccountSettingsScreen() {

    val strings = LocalStrings.current.user
    val stringsCommon = LocalStrings.current.common

    val presenter: IPaymentAccountSettingsPresenter = koinInject()
    val accounts by presenter.accounts.collectAsState()
    val selectedAccount by presenter.selectedAccount.collectAsState()

    var accountName by remember { mutableStateOf(selectedAccount.name) }
    var accountDescription by remember { mutableStateOf(selectedAccount.description) }

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(selectedAccount) {
        accountName = selectedAccount.name
        accountDescription = selectedAccount.description
    }

    if (showBottomSheet) {
        BisqBottomSheet(
            onDismissRequest = { showBottomSheet = !showBottomSheet }
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BisqButton(
                text = strings.user_paymentAccounts_createAccount,
                onClick = { showBottomSheet = !showBottomSheet },
                modifier = Modifier.padding(all = 8.dp)
            )
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
    ) {
        BisqButton(
            text = strings.user_paymentAccounts_createAccount,
            onClick = { showBottomSheet = !showBottomSheet },
            padding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            modifier = Modifier.align(Alignment.End)
        )

        BisqGap.V1()

        BisqEditableDropDown(
            value = accountName,
            onValueChanged = { name ->
                var account = accounts.firstOrNull { it.name == name }

                if (account == null) {
                    account = accounts.firstOrNull { it.description == accountDescription }

                    if (account != null) {
                        account = PaymentAccount(id = account.id, name = name, description = account.description)
                    }
                }

                if (account == null) {
                    account =
                        PaymentAccount(id = accounts.count().toString(), name = name, description = accountDescription)
                }

                presenter.selectAccount(account)
                accountName = account.name
                accountDescription = account.description
            },
            items = accounts.map { it.name },
            label = strings.user_userProfile_payment_account
        )

        BisqGap.V1()

        BisqTextField(
            value = accountDescription,
            onValueChange = { value, isValid -> accountDescription = value },
            label = strings.user_paymentAccounts_accountData,
            isTextArea = true,
            validation = {

                if (it.length < 3) {
                    return@BisqTextField "Min length: 3 characters"
                }

                if (it.length > 1024) {
                    return@BisqTextField "Max length: 1024 characters"
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
                text = stringsCommon.delete_account,
                type = BisqButtonType.Grey,
                onClick = { showConfirmationDialog = true }
            )
            BisqButton(
                text = stringsCommon.buttons_save,
                onClick = {
                    presenter.saveAccount(accountName, accountDescription)
                }
            )
        }
    }

    if (showConfirmationDialog) {
        ConfirmationDialog(
            onConfirm = {
                presenter.deleteCurrentAccount()
                accountName = presenter.selectedAccount.value.name
                accountDescription = presenter.selectedAccount.value.description
                showConfirmationDialog = false
            },
            onDismiss = {
                showConfirmationDialog = false
            }
        )
    }

}

