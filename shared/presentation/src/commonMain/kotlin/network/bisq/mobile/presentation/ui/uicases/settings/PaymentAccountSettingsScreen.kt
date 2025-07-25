package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.domain.data.replicated.account.UserDefinedFiatAccountVO
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.molecules.settings.BreadcrumbNavigation
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
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

@Composable
fun PaymentAccountSettingsScreen() {

    val presenter: IPaymentAccountSettingsPresenter = koinInject()
    val settingsPresenter: ISettingsPresenter = koinInject()

    val accounts by presenter.accounts.collectAsState()
    val selectedAccount by presenter.selectedAccount.collectAsState()

    var accountName by remember { mutableStateOf(selectedAccount?.accountName ?: "") }
    var accountNameValid by remember { mutableStateOf(true) }
    var accountDescription by remember { mutableStateOf(selectedAccount?.accountPayload?.accountData ?: "") }
    var accountDescriptionValid by remember { mutableStateOf(true) }

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val menuTree: MenuItem = settingsPresenter.menuTree()
    val menuPath = remember { mutableStateListOf(menuTree) }

    RememberPresenterLifecycle(presenter, {
        menuPath.add((menuTree as MenuItem.Parent).children[2])
    })

    LaunchedEffect(selectedAccount) {
        accountName = selectedAccount?.accountName ?: ""
        accountDescription = selectedAccount?.accountPayload?.accountData ?: ""
    }

    BisqScrollScaffold(
        topBar = { TopBar("user.paymentAccounts".i18n()) },
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        snackbarHostState = presenter.getSnackState(),
        isInteractive = presenter.isInteractive.collectAsState().value,
        shouldBlurBg = showConfirmationDialog,
    ) {
        if (accounts.isNotEmpty()) {
            BreadcrumbNavigation(path = menuPath) { index ->
                if (index == 0) settingsPresenter.settingsNavigateBack()
            }
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
                verticalArrangement = Arrangement.Top,
            ) {

                BisqText.baseLightGrey("user.paymentAccounts.noAccounts.info".i18n())
                BisqGap.V2()
                BisqText.h2Regular("user.paymentAccounts.noAccounts.whySetup".i18n())
                BisqGap.V1()
                BisqText.baseRegular("user.paymentAccounts.noAccounts.whySetup.info".i18n())
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

                if (it.length > 1024) {
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
            minLines = 2,
            validation = {

                if (it.length < 3) {
                    return@BisqTextField "mobile.user.paymentAccounts.accountData.paymentAccount.validations.minLength".i18n()
                }

                if (it.length > 1024) {
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
                accountName = presenter.selectedAccount.value?.accountName ?: ""
                accountDescription = presenter.selectedAccount.value?.accountPayload?.accountData ?: ""
                showConfirmationDialog = false
            },
            onDismiss = {
                showConfirmationDialog = false
            }
        )
    }

}