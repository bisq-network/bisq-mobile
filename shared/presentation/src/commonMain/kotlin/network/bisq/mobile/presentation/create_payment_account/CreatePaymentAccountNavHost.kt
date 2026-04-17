package network.bisq.mobile.presentation.create_payment_account

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.PaymentMethodVO
import network.bisq.mobile.presentation.common.ui.components.ErrorState
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
    @ConsistentCopyVisibility
    data class PaymentAccountForm private constructor(
        val paymentMethodName: String,
    ) : CreatePaymentAccountRoute {
        constructor(paymentMethod: PaymentMethodVO) : this(paymentMethodName = paymentMethod.name)
    }

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
                        CreatePaymentAccountRoute.PaymentAccountForm(selectedPaymentMethod),
                    )
                },
            )
        }

        composable<CreatePaymentAccountRoute.PaymentAccountForm> { backStackEntry ->
            val route: CreatePaymentAccountRoute.PaymentAccountForm = backStackEntry.toRoute()
            val paymentMethod = runCatching { PaymentMethodVO.valueOf(route.paymentMethodName) }.getOrNull()

            if (paymentMethod == null) {
                ErrorState(message = "mobile.error.generic".i18n())
            } else {
                PaymentAccountFormScreen(
                    paymentMethod = paymentMethod,
                    onContinue = { navController.navigate(CreatePaymentAccountRoute.PaymentAccountReview) },
                )
            }
        }

        composable<CreatePaymentAccountRoute.PaymentAccountReview> {
            PaymentAccountReviewScreen(
                onCreatePaymentAccount = onBack,
            )
        }
    }
}
