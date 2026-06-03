package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.common.presentation.model.account.CryptoPaymentMethodVO
import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodVO
import network.bisq.mobile.client.common.presentation.model.account.PaymentMethodVO
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.common.presentation.model.account.getPaymentTypeVOFromPaymentMethod
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.payment_accounts.domain.model.PaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.UnsupportedAccountState
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.ach_transfer.AchTransferFormContent
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.ach_transfer.AchTransferFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.cash_deposit.CashDepositFormContent
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.cash_deposit.CashDepositFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.monero.MoneroFormContent
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.monero.MoneroFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto.OtherCryptoFormContent
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto.OtherCryptoFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.revolut.RevolutFormContent
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.revolut.RevolutFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.sepa.SepaFormContent
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.sepa.SepaFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise.WiseFormContent
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise.WiseFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle.ZelleFormContent
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle.ZelleFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.ui.PaymentAccountTypeIcon
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware

@Composable
fun PaymentAccountFormScreen(
    paymentMethod: PaymentMethod,
    onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
) {
    val paymentType = getPaymentTypeVOFromPaymentMethod(paymentMethod)
    val paymentMethodVO = paymentMethod.toVO()
    val methodPresenter = rememberMethodPresenter(paymentType)
    val accountNameEntryState = methodPresenter?.uniqueAccountNameEntry?.collectAsState()

    if (paymentMethodVO == null) {
        UnsupportedAccountState(modifier = Modifier.fillMaxWidth())
    } else {
        PaymentAccountFormContent(
            paymentMethod = paymentMethodVO,
            accountNameEntry = accountNameEntryState?.value ?: DataEntry(),
            onAction = { action ->
                methodPresenter?.onCommonAction(action)
            },
            isNextEnabled = methodPresenter != null,
            formContent = {
                PaymentMethodFormContent(
                    paymentMethod = paymentMethod,
                    methodPresenter = methodPresenter,
                    onNavigateToNextScreen = onNavigateToNextScreen,
                )
            },
        )
    }
}

@Composable
private fun PaymentAccountFormContent(
    paymentMethod: PaymentMethodVO,
    accountNameEntry: DataEntry,
    onAction: (AccountFormUiAction) -> Unit,
    isNextEnabled: Boolean,
    formContent: @Composable () -> Unit = {},
) {
    val accountNameHelperText = "mobile.user.paymentAccounts.details.accountName.helper".i18n()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        BisqText.H6Regular("mobile.user.paymentAccounts.details".i18n())
        BisqGap.V1()

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            BisqTextFieldV0(
                value = accountNameEntry.value,
                onValueChange = { onAction(AccountFormUiAction.OnUniqueAccountNameChange(it)) },
                label = "paymentAccounts.summary.accountNameOverlay.accountName.description".i18n(),
                placeholder =
                    "paymentAccounts.createAccount.prompt".i18n(
                        "paymentAccounts.summary.accountNameOverlay.accountName.description".i18n().lowercase(),
                    ),
                isError = accountNameEntry.errorMessage != null,
                bottomMessage = accountNameEntry.errorMessage ?: accountNameHelperText,
                singleLine = true,
            )

            BisqGap.V2()

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                color = BisqTheme.colors.dark_grey40,
            ) {
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                            .padding(BisqUIConstants.ScreenPadding)
                            .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                ) {
                    PaymentAccountTypeIcon(
                        paymentType = paymentMethod.paymentType,
                        size = BisqUIConstants.ScreenPadding2X,
                    )
                    if (paymentMethod is CryptoPaymentMethodVO) {
                        Column {
                            BisqText.BaseRegular(paymentMethod.code)
                            BisqText.BaseRegularGrey(paymentMethod.name)
                        }
                    } else {
                        BisqText.BaseRegularGrey(paymentMethod.name)
                    }
                }
            }

            BisqGap.V1()

            formContent()
        }
        BisqGap.VHalfQuarter()
        BisqButton(
            text = "action.next".i18n(),
            modifier = Modifier.fillMaxWidth(),
            disabled = !isNextEnabled,
            onClick = { onAction(AccountFormUiAction.OnNextClick) },
        )
    }
}

@Composable
private fun PaymentMethodFormContent(
    paymentMethod: PaymentMethod,
    methodPresenter: AccountFormPresenter?,
    onNavigateToNextScreen: (CreatePaymentAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (paymentMethod) {
        is FiatPaymentMethod -> {
            when (paymentMethod.paymentRail) {
                FiatPaymentRail.ACH_TRANSFER -> {
                    val presenter = methodPresenter as? AchTransferFormPresenter
                    if (presenter != null) {
                        AchTransferFormContent(
                            presenter = presenter,
                            onNavigateToNextScreen = onNavigateToNextScreen,
                            modifier = modifier,
                        )
                    } else {
                        UnsupportedAccountState(modifier = modifier.fillMaxWidth())
                    }
                }

                FiatPaymentRail.CASH_DEPOSIT -> {
                    val presenter = methodPresenter as? CashDepositFormPresenter
                    if (presenter != null) {
                        CashDepositFormContent(
                            presenter = presenter,
                            onNavigateToNextScreen = onNavigateToNextScreen,
                            paymentMethod = paymentMethod,
                            modifier = modifier,
                        )
                    } else {
                        UnsupportedAccountState(modifier = modifier.fillMaxWidth())
                    }
                }

                FiatPaymentRail.ZELLE -> {
                    val presenter = methodPresenter as? ZelleFormPresenter
                    if (presenter != null) {
                        ZelleFormContent(
                            presenter = presenter,
                            onNavigateToNextScreen = onNavigateToNextScreen,
                            modifier = modifier,
                        )
                    } else {
                        UnsupportedAccountState(modifier = modifier.fillMaxWidth())
                    }
                }

                FiatPaymentRail.WISE -> {
                    val presenter = methodPresenter as? WiseFormPresenter
                    if (presenter != null) {
                        WiseFormContent(
                            presenter = presenter,
                            onNavigateToNextScreen = onNavigateToNextScreen,
                            paymentMethod = paymentMethod,
                            modifier = modifier,
                        )
                    } else {
                        UnsupportedAccountState(modifier = modifier.fillMaxWidth())
                    }
                }

                FiatPaymentRail.REVOLUT -> {
                    val presenter = methodPresenter as? RevolutFormPresenter
                    if (presenter != null) {
                        RevolutFormContent(
                            presenter = presenter,
                            onNavigateToNextScreen = onNavigateToNextScreen,
                            paymentMethod = paymentMethod,
                            modifier = modifier,
                        )
                    } else {
                        UnsupportedAccountState(modifier = modifier.fillMaxWidth())
                    }
                }

                FiatPaymentRail.SEPA -> {
                    val presenter = methodPresenter as? SepaFormPresenter
                    if (presenter != null) {
                        SepaFormContent(
                            presenter = presenter,
                            onNavigateToNextScreen = onNavigateToNextScreen,
                            paymentMethod = paymentMethod,
                            modifier = modifier,
                        )
                    } else {
                        UnsupportedAccountState(modifier = modifier.fillMaxWidth())
                    }
                }

                else -> UnsupportedAccountState(modifier = modifier.fillMaxWidth())
            }
        }

        is CryptoPaymentMethod -> {
            if (paymentMethod.code.equals("XMR", ignoreCase = true)) {
                val presenter = methodPresenter as? MoneroFormPresenter
                if (presenter != null) {
                    MoneroFormContent(
                        presenter = presenter,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                        paymentMethod = paymentMethod,
                        modifier = modifier,
                    )
                } else {
                    UnsupportedAccountState(modifier = modifier.fillMaxWidth())
                }
            } else {
                val presenter = methodPresenter as? OtherCryptoFormPresenter
                if (presenter != null) {
                    OtherCryptoFormContent(
                        presenter = presenter,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                        paymentMethod = paymentMethod,
                        modifier = modifier,
                    )
                } else {
                    UnsupportedAccountState(modifier = modifier)
                }
            }
        }

        else -> UnsupportedAccountState(modifier = modifier.fillMaxWidth())
    }
}

@Preview
@Composable
private fun UnsupportedPaymentMethodFormStatePreview() {
    BisqTheme.Preview {
        UnsupportedAccountState(modifier = Modifier.fillMaxWidth())
    }
}

@ExcludeFromCoverage
private fun previewPaymentMethodVO(): PaymentMethodVO =
    FiatPaymentMethodVO(
        paymentType = PaymentTypeVO.ZELLE,
        name = "Zelle",
        supportedCurrencyCodes = "",
        countryNames = "",
        chargebackRisk = null,
        tradeDuration = EMPTY_STRING,
        tradeLimitInfo = EMPTY_STRING,
    )

@Preview
@Composable
private fun PaymentAccountFormContent_DefaultPreview() {
    BisqTheme.Preview {
        PaymentAccountFormContent(
            paymentMethod = previewPaymentMethodVO(),
            accountNameEntry = DataEntry(value = "My account"),
            onAction = {},
            isNextEnabled = true,
            formContent = {
                Column {
                    BisqText.BaseRegularGrey("Method-specific form preview")
                }
            },
        )
    }
}

@Preview
@Composable
private fun PaymentAccountFormContent_ErrorPreview() {
    BisqTheme.Preview {
        PaymentAccountFormContent(
            paymentMethod = previewPaymentMethodVO(),
            accountNameEntry =
                DataEntry(
                    value = "a",
                    errorMessage = "validation.tooShortOrTooLong".i18n(3, 100),
                ),
            onAction = {},
            isNextEnabled = false,
            formContent = {
                Column {
                    BisqText.BaseRegularGrey("Method-specific form preview")
                }
            },
        )
    }
}

@Composable
private fun rememberMethodPresenter(paymentType: PaymentTypeVO?): AccountFormPresenter? =
    when (paymentType) {
        PaymentTypeVO.ACH_TRANSFER -> RememberPresenterLifecycleBackStackAware<AchTransferFormPresenter>()
        PaymentTypeVO.CASH_DEPOSIT -> RememberPresenterLifecycleBackStackAware<CashDepositFormPresenter>()
        PaymentTypeVO.ZELLE -> RememberPresenterLifecycleBackStackAware<ZelleFormPresenter>()
        PaymentTypeVO.XMR -> RememberPresenterLifecycleBackStackAware<MoneroFormPresenter>()
        PaymentTypeVO.WISE -> RememberPresenterLifecycleBackStackAware<WiseFormPresenter>()
        PaymentTypeVO.REVOLUT -> RememberPresenterLifecycleBackStackAware<RevolutFormPresenter>()
        PaymentTypeVO.SEPA -> RememberPresenterLifecycleBackStackAware<SepaFormPresenter>()
        PaymentTypeVO.BSQ,
        PaymentTypeVO.LTC,
        PaymentTypeVO.ETH,
        PaymentTypeVO.ETC,
        PaymentTypeVO.LBTC,
        PaymentTypeVO.LNBTC,
        PaymentTypeVO.GRIN,
        PaymentTypeVO.ZEC,
        PaymentTypeVO.DOGE,
        -> RememberPresenterLifecycleBackStackAware<OtherCryptoFormPresenter>()

        else -> null
    }
