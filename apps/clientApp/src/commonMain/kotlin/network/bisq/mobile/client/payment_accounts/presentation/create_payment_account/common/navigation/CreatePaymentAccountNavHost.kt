package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.crypto.SelectCryptoPaymentMethodScreen
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.fiat.SelectFiatPaymentMethodScreen
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.PaymentAccountFormScreen
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step3_account_review.PaymentAccountReviewScreen
import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@ExcludeFromCoverage
sealed interface CreatePaymentAccountRoute {
    @Serializable
    data object SelectCryptoPaymentMethod : CreatePaymentAccountRoute

    @Serializable
    data object SelectFiatPaymentMethod : CreatePaymentAccountRoute

    @Serializable
    data object PaymentAccountForm : CreatePaymentAccountRoute

    @Serializable
    data object PaymentAccountReview : CreatePaymentAccountRoute
}

@ExcludeFromCoverage
@Composable
fun CreatePaymentAccountNavHost(
    navController: NavHostController,
    accountType: PaymentAccountType,
    createPaymentAccount: CreatePaymentAccount?,
    paymentMethod: PaymentMethod?,
    onNavigateFromSelectPaymentMethod: (PaymentMethod) -> Unit,
    onNavigateFromPaymentAccountForm: (CreatePaymentAccount) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val startDestination =
        when (accountType) {
            PaymentAccountType.FIAT -> CreatePaymentAccountRoute.SelectFiatPaymentMethod
            PaymentAccountType.CRYPTO -> CreatePaymentAccountRoute.SelectCryptoPaymentMethod
        }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<CreatePaymentAccountRoute.SelectCryptoPaymentMethod> {
            SelectCryptoPaymentMethodScreen(
                onNavigateToNextScreen = onNavigateFromSelectPaymentMethod,
            )
        }

        composable<CreatePaymentAccountRoute.SelectFiatPaymentMethod> {
            SelectFiatPaymentMethodScreen(
                onNavigateToNextScreen = onNavigateFromSelectPaymentMethod,
            )
        }

        composable<CreatePaymentAccountRoute.PaymentAccountForm> {
            if (paymentMethod != null) {
                PaymentAccountFormScreen(
                    paymentMethod = paymentMethod,
                    onNavigateToNextScreen = onNavigateFromPaymentAccountForm,
                )
            } else {
                ErrorState(message = "mobile.error.generic".i18n())
            }
        }

        composable<CreatePaymentAccountRoute.PaymentAccountReview> {
            if (createPaymentAccount != null && paymentMethod != null) {
                PaymentAccountReviewScreen(
                    onCloseCreateAccountFlow = onBack,
                    createPaymentAccount = createPaymentAccount,
                    paymentMethod = paymentMethod,
                )
            } else {
                ErrorState(message = "mobile.error.generic".i18n())
            }
        }
    }
}
