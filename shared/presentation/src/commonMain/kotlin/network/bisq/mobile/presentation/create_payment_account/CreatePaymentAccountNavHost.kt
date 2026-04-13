package network.bisq.mobile.presentation.create_payment_account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.navigation.paymentMethodNavType
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.create_payment_account.account_review.PaymentAccountReviewScreen
import network.bisq.mobile.presentation.create_payment_account.payment_accout_form.PaymentAccountFormScreen
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.SelectPaymentMethodScreen
import kotlin.reflect.typeOf

@ExcludeFromCoverage
sealed interface CreatePaymentAccountRoute {
    @Serializable
    data object SelectPaymentMethod : CreatePaymentAccountRoute

    @Serializable
    data class PaymentAccountForm(
        val paymentMethod: PaymentMethodVO,
    ) : CreatePaymentAccountRoute

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
                onContinue = { selectedPaymentMethod ->
                    navController.navigate(
                        CreatePaymentAccountRoute.PaymentAccountForm(
                            paymentMethod = selectedPaymentMethod,
                        ),
                    )
                },
            )
        }

        composable<CreatePaymentAccountRoute.PaymentAccountForm>(
            typeMap = mapOf(typeOf<PaymentMethodVO>() to paymentMethodNavType),
        ) { backStackEntry ->
            val route: CreatePaymentAccountRoute.PaymentAccountForm = backStackEntry.toRoute()
            PaymentAccountFormScreen(
                paymentMethod = route.paymentMethod,
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
