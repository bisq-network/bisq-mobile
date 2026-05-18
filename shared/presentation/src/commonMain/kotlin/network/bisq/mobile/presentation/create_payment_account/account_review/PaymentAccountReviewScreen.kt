package network.bisq.mobile.presentation.create_payment_account.account_review

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.OtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.fiat.WiseAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.MoneroAccountDetailContent
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.OtherCryptoAssetAccountDetailContent
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.ZelleAccountDetailContent
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountDetailFieldRow
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.wise.WiseAccountDetailContent
import network.bisq.mobile.presentation.create_payment_account.ui.UnsupportedAccountState

@Composable
fun PaymentAccountReviewScreen(
    createPaymentAccount: CreatePaymentAccount,
    paymentMethod: PaymentMethod,
    onCloseCreateAccountFlow: () -> Unit = {},
) {
    val presenter = RememberPresenterLifecycleBackStackAware<PaymentAccountReviewPresenter>()
    val uiState by presenter.uiState.collectAsState()
    val latestOnCloseCreateAccountFlow = rememberUpdatedState(onCloseCreateAccountFlow)

    LaunchedEffect(presenter, createPaymentAccount, paymentMethod) {
        presenter.initialize(
            createPaymentAccount = createPaymentAccount,
            paymentMethod = paymentMethod,
        )
    }

    LaunchedEffect(presenter) {
        presenter.effect.collect { effect ->
            when (effect) {
                PaymentAccountReviewEffect.CloseCreateAccountFlow -> latestOnCloseCreateAccountFlow.value()
            }
        }
    }

    val paymentAccount = uiState.paymentAccount
    when {
        uiState.isLoading -> LoadingState()
        paymentAccount != null ->
            PaymentAccountReviewContent(
                paymentAccount = paymentAccount,
                onCreateAccountClick = {
                    presenter.onAction(PaymentAccountReviewUiAction.OnCreateAccountClick(createPaymentAccount))
                },
            )

        else -> UnsupportedAccountState(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun PaymentAccountReviewContent(
    paymentAccount: PaymentAccount,
    onCreateAccountClick: () -> Unit = {},
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BisqText.H6Regular("mobile.user.paymentAccounts.review".i18n())
        BisqGap.V1()

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

                is OtherCryptoAssetAccount ->
                    OtherCryptoAssetAccountDetailContent(paymentAccount)

                is WiseAccount ->
                    WiseAccountDetailContent(paymentAccount)

                else -> UnsupportedAccountState(modifier = Modifier.fillMaxWidth())
            }
        }

        BisqGap.VHalfQuarter()
        BisqButton(
            text = "paymentAccounts.createAccount.createAccount".i18n(),
            modifier = Modifier.fillMaxWidth(),
            onClick = onCreateAccountClick,
        )
    }
}

private val previewPaymentAccount =
    ZelleAccount(
        accountName = "Zelle Main",
        accountPayload =
            ZelleAccountPayload(
                holderName = "Alice Doe",
                emailOrMobileNr = "alice@example.com",
                paymentMethodName = "Zelle",
                currency = "USD",
                country = "United States",
            ),
    )

@Preview
@Composable
private fun PaymentAccountReviewScreenPreview() {
    BisqTheme.Preview {
        PaymentAccountReviewContent(
            paymentAccount = previewPaymentAccount,
            onCreateAccountClick = {},
        )
    }
}
