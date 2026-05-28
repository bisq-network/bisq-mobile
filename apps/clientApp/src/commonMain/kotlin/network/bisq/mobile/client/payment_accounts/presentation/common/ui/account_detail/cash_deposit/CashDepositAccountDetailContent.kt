package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.cash_deposit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.common.presentation.model.account.toVO
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.cash_deposit.CashDepositAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailDetailsSection
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailHeader
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.ExpandableAccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.FiatChargebackRiskBadge
import network.bisq.mobile.client.payment_accounts.presentation.common.util.toDisplayString
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware

@Composable
fun CashDepositAccountDetailContent(account: CashDepositAccount) {
    val presenter = RememberPresenterLifecycleBackStackAware<CashDepositAccountDetailPresenter>()
    val uiState by presenter.uiState.collectAsState()

    LaunchedEffect(account) {
        presenter.initialize(account)
    }

    CashDepositAccountDetailContent(
        account = account,
        uiState = uiState,
        onRetryLoadCountryDetails = { presenter.initialize(account) },
    )
}

@Composable
private fun CashDepositAccountDetailContent(
    account: CashDepositAccount,
    uiState: CashDepositAccountDetailUiState,
    onRetryLoadCountryDetails: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey40,
    ) {
        Column {
            AccountDetailHeader(
                paymentType = PaymentTypeVO.CASH_DEPOSIT,
                primaryText = account.accountPayload.paymentMethodName,
            )

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                when {
                    uiState.isLoadingCountryDetails -> {
                        LoadingState(
                            paddingValues = PaddingValues(vertical = 32.dp),
                        )
                    }

                    uiState.isCountryDetailsError -> {
                        ErrorState(
                            paddingValues = PaddingValues(vertical = 24.dp),
                            onRetry = onRetryLoadCountryDetails,
                        )
                    }

                    else -> {
                        val countryDetails = uiState.countryDetails

                        AccountDetailFieldRow(
                            label = "paymentAccounts.country".i18n(),
                            value = account.accountPayload.country.name,
                        )
                        AccountDetailFieldRow(
                            label = "paymentAccounts.currency".i18n(),
                            value = account.accountPayload.currency.toDisplayString(),
                        )
                        AccountDetailFieldRow(
                            label = "paymentAccounts.holderName".i18n(),
                            value = account.accountPayload.holderName,
                        )
                        account.accountPayload.holderId?.let { holderId ->
                            AccountDetailFieldRow(
                                label = countryDetails?.holderIdDescription.orFallback("paymentAccounts.bank.holderId"),
                                value = holderId,
                            )
                        }
                        AccountDetailFieldRow(
                            label = "paymentAccounts.bank.bankName".i18n(),
                            value = account.accountPayload.bankName,
                        )
                        account.accountPayload.bankId?.let { bankId ->
                            AccountDetailFieldRow(
                                label = countryDetails?.bankIdDescription.orFallback("paymentAccounts.bank.bankId"),
                                value = bankId,
                            )
                        }
                        account.accountPayload.branchId?.let { branchId ->
                            AccountDetailFieldRow(
                                label = countryDetails?.branchIdDescription.orFallback("paymentAccounts.bank.branchId"),
                                value = branchId,
                            )
                        }
                        AccountDetailFieldRow(
                            label = countryDetails?.accountNrDescription.orFallback("paymentAccounts.accountNr"),
                            value = account.accountPayload.accountNr,
                        )
                        account.accountPayload.bankAccountType?.let { bankAccountType ->
                            AccountDetailFieldRow(
                                label = "paymentAccounts.bank.bankAccountType".i18n(),
                                value = bankAccountType.toDisplayString(),
                            )
                        }
                        account.accountPayload.nationalAccountId?.let { nationalAccountId ->
                            AccountDetailFieldRow(
                                label =
                                    countryDetails
                                        ?.nationalAccountIdDescription
                                        .orFallback("paymentAccounts.bank.accountNrOrIban"),
                                value = nationalAccountId,
                            )
                        }
                        account.accountPayload.requirements?.let { requirements ->
                            ExpandableAccountDetailFieldRow(
                                label = "paymentAccounts.cashDeposit.requirements".i18n(),
                                value = requirements,
                            )
                        }

                        AccountDetailDetailsSection(
                            creationDate = account.creationDate,
                            tradeLimitInfo = account.tradeLimitInfo,
                            tradeDuration = account.tradeDuration,
                        )

                        account.accountPayload.chargebackRisk?.let { risk ->
                            BisqGap.VQuarter()
                            risk.toVO()?.let { riskVO -> FiatChargebackRiskBadge(risk = riskVO) }
                        }
                    }
                }
            }
        }
    }
}

private fun String?.orFallback(fallbackKey: String): String = takeUnless { it.isNullOrBlank() } ?: fallbackKey.i18n()

private val previewAccount =
    CashDepositAccount(
        accountName = "Cash Deposit Main",
        accountPayload =
            CashDepositAccountPayload(
                chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
                paymentMethodName = "Cash Deposit",
                currency = FiatCurrency("USD", "US Dollar"),
                country = Country("US", "United States"),
                holderName = "Alice Doe",
                holderId = "1234",
                bankName = "Bisq Bank",
                bankId = "BANKUS33",
                branchId = "001",
                accountNr = "123456789",
                bankAccountType = BankAccountType.CHECKING,
                nationalAccountId = "NAT-123",
                requirements = "Bring cash deposit receipt.",
            ),
        creationDate = "Apr 3, 2026",
        tradeLimitInfo = "5000.00 USD",
        tradeDuration = "4 days",
    )

@Preview
@Composable
private fun CashDepositAccountDetailContentPreview() {
    BisqTheme.Preview {
        CashDepositAccountDetailContent(
            account = previewAccount,
            uiState = CashDepositAccountDetailUiState(),
            onRetryLoadCountryDetails = {},
        )
    }
}
