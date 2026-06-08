package network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.bank

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
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.SameBankAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.same_bank.SameBankAccountPayload
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailDetailsSection
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailHeader
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
fun BankAccountDetailContent(
    account: BankAccount,
    paymentType: PaymentTypeVO,
) {
    val presenter = RememberPresenterLifecycleBackStackAware<BankAccountDetailPresenter>()
    val uiState by presenter.uiState.collectAsState()

    LaunchedEffect(account) {
        presenter.initialize(account)
    }

    BankAccountDetailContent(
        account = account,
        paymentType = paymentType,
        uiState = uiState,
        onRetryLoadCountryDetails = { presenter.initialize(account) },
    )
}

@Composable
private fun BankAccountDetailContent(
    account: BankAccount,
    paymentType: PaymentTypeVO,
    uiState: BankAccountDetailUiState,
    onRetryLoadCountryDetails: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey40,
    ) {
        Column {
            AccountDetailHeader(
                paymentType = paymentType,
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
                        val payload = account.accountPayload
                        val countryDetails = uiState.countryDetails

                        AccountDetailFieldRow(
                            label = "paymentAccounts.country".i18n(),
                            value = payload.country.name,
                        )
                        AccountDetailFieldRow(
                            label = "paymentAccounts.currency".i18n(),
                            value = payload.currency.toDisplayString(),
                        )
                        payload.holderName?.let { holderName ->
                            AccountDetailFieldRow(
                                label = "paymentAccounts.holderName".i18n(),
                                value = holderName,
                            )
                        }
                        payload.holderId?.let { holderId ->
                            AccountDetailFieldRow(
                                label = countryDetails?.holderIdDescription.orFallback("paymentAccounts.bank.holderId"),
                                value = holderId,
                            )
                        }
                        payload.bankName?.let { bankName ->
                            AccountDetailFieldRow(
                                label = "paymentAccounts.bank.bankName".i18n(),
                                value = bankName,
                            )
                        }
                        payload.bankId?.let { bankId ->
                            AccountDetailFieldRow(
                                label = countryDetails?.bankIdDescription.orFallback("paymentAccounts.bank.bankId"),
                                value = bankId,
                            )
                        }
                        payload.branchId?.let { branchId ->
                            AccountDetailFieldRow(
                                label = countryDetails?.branchIdDescription.orFallback("paymentAccounts.bank.branchId"),
                                value = branchId,
                            )
                        }
                        AccountDetailFieldRow(
                            label = countryDetails?.accountNrDescription.orFallback("paymentAccounts.accountNr"),
                            value = payload.accountNr,
                        )
                        payload.bankAccountType?.let { bankAccountType ->
                            AccountDetailFieldRow(
                                label = "paymentAccounts.bank.bankAccountType".i18n(),
                                value = bankAccountType.toDisplayString(),
                            )
                        }
                        payload.nationalAccountId?.let { nationalAccountId ->
                            AccountDetailFieldRow(
                                label =
                                    countryDetails
                                        ?.nationalAccountIdDescription
                                        .orFallback("paymentAccounts.bank.accountNrOrIban"),
                                value = nationalAccountId,
                            )
                        }

                        AccountDetailDetailsSection(
                            creationDate = account.creationDate,
                            tradeLimitInfo = account.tradeLimitInfo,
                            tradeDuration = account.tradeDuration,
                        )

                        payload.chargebackRisk?.let { risk ->
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
    SameBankAccount(
        accountName = "Same Bank Main",
        accountPayload =
            SameBankAccountPayload(
                chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
                paymentMethodName = "Same Bank",
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
            ),
        creationDate = "Apr 3, 2026",
        tradeLimitInfo = "5000.00 USD",
        tradeDuration = "5 days",
    )

@Preview
@Composable
private fun BankAccountDetailContentPreview() {
    BisqTheme.Preview {
        BankAccountDetailContent(
            account = previewAccount,
            paymentType = PaymentTypeVO.SAME_BANK,
            uiState = BankAccountDetailUiState(),
            onRetryLoadCountryDetails = {},
        )
    }
}
