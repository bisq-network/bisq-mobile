package network.bisq.mobile.presentation.create_payment_account.account_review

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.MoneroAccountReviewContent
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.ZelleAccountReviewContent
import network.bisq.mobile.presentation.create_payment_account.account_review.ui.core.AccountReviewFieldRow
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.PaymentMethodVO

@ExcludeFromCoverage
@Composable
fun PaymentAccountReviewScreen(
    paymentAccount: PaymentAccount,
    paymentMethod: PaymentMethodVO,
    onCloseCreateAccountFlow: () -> Unit = {},
) {
    val presenter = RememberPresenterLifecycleBackStackAware<PaymentAccountReviewPresenter>()
    val latestOnCloseCreateAccountFlow = rememberUpdatedState(onCloseCreateAccountFlow)

    LaunchedEffect(presenter) {
        presenter.effect.collect { effect ->
            when (effect) {
                PaymentAccountReviewEffect.CloseCreateAccountFlow -> latestOnCloseCreateAccountFlow.value()
            }
        }
    }

    PaymentAccountReviewContent(
        paymentAccount = paymentAccount,
        paymentMethod = paymentMethod,
        onCreateAccountClick = {
            presenter.onAction(PaymentAccountReviewUiAction.OnCreateAccountClick(paymentAccount))
        },
    )
}

@Composable
fun PaymentAccountReviewContent(
    paymentAccount: PaymentAccount,
    paymentMethod: PaymentMethodVO,
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

        AccountReviewFieldRow(
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
                is ZelleAccount if paymentMethod is FiatPaymentMethodVO ->
                    ZelleAccountReviewContent(
                        paymentMethod = paymentMethod,
                        account = paymentAccount,
                    )

                is MoneroAccount if paymentMethod is CryptoPaymentMethodVO ->
                    MoneroAccountReviewContent(
                        paymentMethod = paymentMethod,
                        account = paymentAccount,
                    )

                else -> Unit
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

private val previewPaymentMethod =
    FiatPaymentMethodVO(
        paymentType = PaymentTypeVO.ZELLE,
        name = "Zelle",
        supportedCurrencyCodes = "USD",
        countryNames = "United States",
        chargebackRisk = null,
        restrictions = EMPTY_STRING,
    )

private val previewPaymentAccount =
    ZelleAccount(
        accountName = "Alice Doe",
        accountPayload =
            ZelleAccountPayload(
                holderName = "Alice Doe",
                emailOrMobileNr = "alice@example.com",
            ),
    )

@Preview
@Composable
private fun PaymentAccountReviewScreenPreview() {
    BisqTheme.Preview {
        PaymentAccountReviewContent(
            paymentAccount = previewPaymentAccount,
            paymentMethod = previewPaymentMethod,
            onCreateAccountClick = {},
        )
    }
}
