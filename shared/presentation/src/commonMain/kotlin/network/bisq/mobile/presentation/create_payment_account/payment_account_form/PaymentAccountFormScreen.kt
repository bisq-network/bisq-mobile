package network.bisq.mobile.presentation.create_payment_account.payment_account_form

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
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
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
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.AccountFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero.MoneroFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero.MoneroPaymentAccountFormContent
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle.ZelleFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle.ZellePaymentAccountFormContent
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.FiatPaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.PaymentMethodVO
import network.bisq.mobile.presentation.create_payment_account.ui.UnsupportedAccountState
import network.bisq.mobile.presentation.settings.payment_accounts_musig.ui.PaymentAccountTypeIcon

@ExcludeFromCoverage
@Composable
fun PaymentAccountFormScreen(
    paymentMethod: PaymentMethodVO,
    onNavigateToNextScreen: (PaymentAccount) -> Unit = {},
) {
    val methodPresenter = rememberMethodPresenter(paymentMethod)
    val accountNameEntryState = methodPresenter?.uniqueAccountNameEntry?.collectAsState()

    PaymentAccountFormContent(
        paymentMethod = paymentMethod,
        accountNameEntry = accountNameEntryState?.value ?: DataEntry(),
        onAction = { action ->
            methodPresenter?.onAction(action)
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

@Composable
fun PaymentAccountFormContent(
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
                    BisqText.BaseRegularGrey(paymentMethod.name)
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
    paymentMethod: PaymentMethodVO,
    methodPresenter: AccountFormPresenter?,
    onNavigateToNextScreen: (PaymentAccount) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (paymentMethod.paymentType) {
        PaymentTypeVO.ZELLE -> {
            val presenter = methodPresenter as? ZelleFormPresenter
            val fiatPaymentMethod = paymentMethod as? FiatPaymentMethodVO
            if (presenter != null && fiatPaymentMethod != null) {
                ZellePaymentAccountFormContent(
                    presenter = presenter,
                    paymentMethod = fiatPaymentMethod,
                    onNavigateToNextScreen = onNavigateToNextScreen,
                    modifier = modifier,
                )
            } else {
                UnsupportedAccountState(modifier = modifier.fillMaxWidth())
            }
        }

        PaymentTypeVO.XMR -> {
            val presenter = methodPresenter as? MoneroFormPresenter
            val cryptoPaymentMethod = paymentMethod as? CryptoPaymentMethodVO
            if (presenter != null && cryptoPaymentMethod != null) {
                MoneroPaymentAccountFormContent(
                    presenter = presenter,
                    onNavigateToNextScreen = onNavigateToNextScreen,
                    paymentMethod = cryptoPaymentMethod,
                    modifier = modifier,
                )
            } else {
                UnsupportedAccountState(modifier = modifier.fillMaxWidth())
            }
        }

        else -> {
            UnsupportedAccountState(modifier = modifier.fillMaxWidth())
        }
    }
}

@Preview
@Composable
private fun UnsupportedPaymentMethodFormStatePreview() {
    BisqTheme.Preview {
        UnsupportedAccountState(modifier = Modifier.fillMaxWidth())
    }
}

@Preview
@Composable
private fun PaymentAccountFormContentPreview_DefaultPreview() {
    val paymentMethod =
        FiatPaymentMethodVO(
            paymentType = PaymentTypeVO.ZELLE,
            name = "Zelle",
            supportedCurrencyCodes = "",
            countryNames = "",
            chargebackRisk = null,
            tradeDuration = EMPTY_STRING,
            tradeLimitInfo = EMPTY_STRING,
        )
    BisqTheme.Preview {
        PaymentAccountFormContent(
            paymentMethod = paymentMethod,
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
private fun PaymentAccountFormContentPreview_ErrorPreview() {
    val paymentMethod =
        FiatPaymentMethodVO(
            paymentType = PaymentTypeVO.ZELLE,
            name = "Zelle",
            supportedCurrencyCodes = "",
            countryNames = "",
            chargebackRisk = null,
            tradeDuration = EMPTY_STRING,
            tradeLimitInfo = EMPTY_STRING,
        )
    BisqTheme.Preview {
        PaymentAccountFormContent(
            paymentMethod = paymentMethod,
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
private fun rememberMethodPresenter(paymentMethod: PaymentMethodVO): AccountFormPresenter? =
    when (paymentMethod.paymentType) {
        PaymentTypeVO.ZELLE -> RememberPresenterLifecycleBackStackAware<ZelleFormPresenter>()
        PaymentTypeVO.XMR -> RememberPresenterLifecycleBackStackAware<MoneroFormPresenter>()
        else -> null
    }
