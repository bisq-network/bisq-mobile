package network.bisq.mobile.presentation.create_payment_account

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.navigation.types.PaymentAccountType
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.koin.compose.koinInject

@ExcludeFromCoverage
@Composable
fun CreatePaymentAccountScreen(
    accountType: PaymentAccountType,
) {
    val flowNavController = rememberNavController()
    val navigationManager: NavigationManager = koinInject()
    val backStackEntry by flowNavController.currentBackStackEntryAsState()

    val topBarState = getCreatePaymentAccountTopBarState(backStackEntry?.destination, accountType)

    BisqScaffold(
        topBar = {
            CreatePaymentAccountTopBar(
                title = topBarState.title,
                showCloseIcon = topBarState.showCloseIcon,
                onBackClick = {
                    if (!flowNavController.popBackStack()) {
                        navigationManager.navigateBack()
                    }
                },
                onCloseClick = {
                    navigationManager.navigateBack()
                },
            )
        },
    ) { paddingValues ->
        CreatePaymentAccountNavHost(
            navController = flowNavController,
            accountType = accountType,
            onBack = { navigationManager.navigateBack() },
            modifier = Modifier.padding(paddingValues),
        )
    }
}

private data class CreatePaymentAccountTopBarState(
    val title: String,
    val showCloseIcon: Boolean,
)

private fun getCreatePaymentAccountTopBarState(
    destination: NavDestination?,
    accountType: PaymentAccountType,
): CreatePaymentAccountTopBarState {
    val isFormOrReview =
        destination?.hasRoute<CreatePaymentAccountRoute.PaymentAccountForm>() == true ||
            destination?.hasRoute<CreatePaymentAccountRoute.PaymentAccountReview>() == true

    val title =
        when {
            destination?.hasRoute<CreatePaymentAccountRoute.PaymentAccountForm>() == true ->
                when (accountType) {
                    PaymentAccountType.FIAT -> "paymentAccounts.progress.accountData".i18n()
                    PaymentAccountType.CRYPTO -> "paymentAccounts.crypto.progress.addressAndOptions".i18n()
                }

            destination?.hasRoute<CreatePaymentAccountRoute.PaymentAccountReview>() == true ->
                "paymentAccounts.createAccount.progress.summary".i18n()

            else ->
                when (accountType) {
                    PaymentAccountType.FIAT -> "paymentAccounts.createAccount.paymentMethod.headline".i18n()
                    PaymentAccountType.CRYPTO -> "paymentAccounts.crypto.paymentMethod.headline".i18n()
                }
        }

    return CreatePaymentAccountTopBarState(
        title = title,
        showCloseIcon = isFormOrReview,
    )
}
