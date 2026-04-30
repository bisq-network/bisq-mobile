package network.bisq.mobile.presentation.create_payment_account.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.ErrorState
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.create_payment_account.account_review.PaymentAccountReviewScreen
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.PaymentAccountFormScreen
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.crypto.SelectCryptoPaymentMethodScreen
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.fiat.SelectFiatPaymentMethodScreen
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.PaymentMethodVO

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
    paymentAccount: PaymentAccount?,
    paymentMethod: PaymentMethodVO?,
    onNavigateFromSelectPaymentMethod: (PaymentMethodVO) -> Unit,
    onNavigateFromPaymentAccountForm: (PaymentAccount) -> Unit,
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
            if (paymentAccount != null && paymentMethod != null) {
                PaymentAccountReviewScreen(
                    onCloseCreateAccountFlow = onBack,
                    paymentAccount = paymentAccount,
                    paymentMethod = paymentMethod,
                )
            } else {
                ErrorState(message = "mobile.error.generic".i18n())
            }
        }
    }
}
