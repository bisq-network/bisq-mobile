package network.bisq.mobile.presentation.create_payment_account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.create_payment_account.account_review.PaymentAccountReviewScreen
import network.bisq.mobile.presentation.create_payment_account.payment_accout_form.PaymentAccountFormScreen
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.SelectPaymentMethodScreen

@ExcludeFromCoverage
sealed interface CreatePaymentAccountRoute {
    @Serializable
    data object SelectPaymentMethod : CreatePaymentAccountRoute

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
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = CreatePaymentAccountRoute.SelectPaymentMethod,
        modifier = modifier,
    ) {
        composable<CreatePaymentAccountRoute.SelectPaymentMethod> {
            SelectPaymentMethodScreen(
                accountType = accountType,
                onContinue = { navController.navigate(CreatePaymentAccountRoute.PaymentAccountForm) },
            )
        }

        composable<CreatePaymentAccountRoute.PaymentAccountForm> {
            PaymentAccountFormScreen(
                onContinue = { navController.navigate(CreatePaymentAccountRoute.PaymentAccountReview) },
            )
        }

        composable<CreatePaymentAccountRoute.PaymentAccountReview> {
            PaymentAccountReviewScreen(
                onCreatePaymentAccount = onBack,
            )
        }
    }
}
