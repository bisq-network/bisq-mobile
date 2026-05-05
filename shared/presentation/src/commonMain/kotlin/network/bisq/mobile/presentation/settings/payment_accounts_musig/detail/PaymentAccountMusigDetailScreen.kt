package network.bisq.mobile.presentation.settings.payment_accounts_musig.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.PaymentAccountPayload
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload
import network.bisq.mobile.domain.model.account.fiat.UserDefinedFiatAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.MoneroAccountDetailContent
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.UserDefinedAccountDetailContent
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.ZelleAccountDetailContent
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountDetailFieldRow
import network.bisq.mobile.presentation.create_payment_account.ui.UnsupportedAccountState

@ExcludeFromCoverage
@Composable
fun PaymentAccountMusigDetailScreen(
    accountName: String,
) {
    val presenter = RememberPresenterLifecycleBackStackAware<PaymentAccountMusigDetailPresenter>()
    val uiState by presenter.uiState.collectAsState()

    LaunchedEffect(presenter, accountName) {
        presenter.initialize(accountName)
    }

    PaymentAccountMusigDetailContent(
        uiState = uiState,
        topBar = { TopBar("mobile.user.paymentAccounts.details".i18n()) },
        onAction = presenter::onAction,
    )
}

@Composable
fun PaymentAccountMusigDetailContent(
    uiState: PaymentAccountMusigDetailUiState,
    onAction: (PaymentAccountMusigDetailUiAction) -> Unit,
    topBar: @Composable () -> Unit = {},
) {
    val paymentAccount = uiState.paymentAccount

    BisqScaffold(
        topBar = topBar,
    ) { paddingValues ->
        when {
            uiState.isAccountMissing ->
                ErrorState(
                    paddingValues = paddingValues,
                    message = "mobile.error.generic".i18n(),
                )

            paymentAccount != null -> {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    AccountDetailFieldRow(
                        label = "paymentAccounts.summary.accountNameOverlay.accountName.description".i18n(),
                        value = paymentAccount.accountName,
                    )
                    BisqGap.V1()

                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                    ) {
                        when (paymentAccount) {
                            is ZelleAccount ->
                                ZelleAccountDetailContent(paymentAccount)

                            is MoneroAccount ->
                                MoneroAccountDetailContent(paymentAccount)

                            is UserDefinedFiatAccount ->
                                UserDefinedAccountDetailContent(paymentAccount)

                            else -> UnsupportedAccountState(modifier = Modifier.fillMaxSize())
                        }
                    }

                    BisqGap.VHalfQuarter()
                    BisqButton(
                        text = "mobile.action.delete".i18n(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick) },
                    )
                }

                if (uiState.showDeleteConfirmationDialog) {
                    ConfirmationDialog(
                        headline = "mobile.user.paymentAccounts.delete.confirmation".i18n(),
                        onConfirm = { onAction(PaymentAccountMusigDetailUiAction.OnConfirmDeleteAccountClick) },
                        onDismiss = { onAction(PaymentAccountMusigDetailUiAction.OnCancelDeleteAccountClick) },
                    )
                }
            }

            else -> {
                LoadingState(paddingValues)
            }
        }
    }
}

@ExcludeFromCoverage
@Composable
private fun PreviewTopBar() {
    TopBarContent(
        title = "mobile.user.paymentAccounts.details".i18n(),
        showBackButton = true,
        showUserAvatar = true,
    )
}

private val previewZelleAccount =
    ZelleAccount(
        accountName = "Alice Doe",
        accountPayload =
            ZelleAccountPayload(
                holderName = "Alice Doe",
                emailOrMobileNr = "alice@example.com",
                paymentMethodName = "Zelle",
                currency = "USD",
                country = "US",
            ),
        creationDate = null,
        tradeLimitInfo = "1000 USD",
        tradeDuration = "1 day",
    )

private val previewMoneroAccount =
    MoneroAccount(
        accountName = "My Monero Account",
        accountPayload =
            MoneroAccountPayload(
                address = "44AFFq5kSiGBoZ...",
                isInstant = false,
                isAutoConf = true,
                autoConfNumConfirmations = 10,
                autoConfMaxTradeAmount = 200000,
                autoConfExplorerUrls = "https://example.com/explorer",
                useSubAddresses = true,
                mainAddress = "44AFFq5kSiGBoZ...",
                privateViewKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                subAddress = "89ABCDE...",
                accountIndex = 0,
                initialSubAddressIndex = 0,
                currencyCode = "XMR",
                currencyName = "Monero",
                supportAutoConf = true,
            ),
        creationDate = null,
        tradeLimitInfo = null,
        tradeDuration = null,
    )

private val previewUnsupportedAccount =
    object : PaymentAccount {
        override val accountName: String = "Unsupported"
        override val accountPayload: PaymentAccountPayload = object : PaymentAccountPayload {}
        override val creationDate: String? = null
        override val tradeLimitInfo: String? = null
        override val tradeDuration: String? = null
    }

@Preview
@Composable
private fun PaymentAccountMusigDetail_ZelleLoadedPreview() {
    BisqTheme.Preview {
        PaymentAccountMusigDetailContent(
            uiState =
                PaymentAccountMusigDetailUiState(
                    paymentAccount = previewZelleAccount,
                    isAccountMissing = false,
                ),
            onAction = {},
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountMusigDetail_MoneroLoadedPreview() {
    BisqTheme.Preview {
        PaymentAccountMusigDetailContent(
            uiState =
                PaymentAccountMusigDetailUiState(
                    paymentAccount = previewMoneroAccount,
                    isAccountMissing = false,
                ),
            onAction = {},
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountMusigDetail_ErrorPreview() {
    BisqTheme.Preview {
        PaymentAccountMusigDetailContent(
            uiState = PaymentAccountMusigDetailUiState(isAccountMissing = true),
            onAction = {},
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountMusigDetail_UnsupportedAccountPreview() {
    BisqTheme.Preview {
        PaymentAccountMusigDetailContent(
            uiState =
                PaymentAccountMusigDetailUiState(
                    paymentAccount = previewUnsupportedAccount,
                    isAccountMissing = false,
                ),
            onAction = {},
            topBar = { PreviewTopBar() },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountMusigDetail_DeleteConfirmationPreview() {
    BisqTheme.Preview {
        PaymentAccountMusigDetailContent(
            uiState =
                PaymentAccountMusigDetailUiState(
                    paymentAccount = previewZelleAccount,
                    isAccountMissing = false,
                    showDeleteConfirmationDialog = true,
                ),
            onAction = {},
            topBar = { PreviewTopBar() },
        )
    }
}
