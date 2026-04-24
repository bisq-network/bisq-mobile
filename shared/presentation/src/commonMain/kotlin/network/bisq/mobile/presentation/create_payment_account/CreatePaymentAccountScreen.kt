package network.bisq.mobile.presentation.create_payment_account

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.create_payment_account.core.navigation.CreatePaymentAccountNavHost
import network.bisq.mobile.presentation.create_payment_account.core.navigation.CreatePaymentAccountRoute
import network.bisq.mobile.presentation.create_payment_account.core.ui.CreatePaymentAccountTopBar
import org.koin.compose.koinInject

@ExcludeFromCoverage
@Composable
fun CreatePaymentAccountScreen(
    accountType: PaymentAccountType,
) {
    val presenter = RememberPresenterLifecycleBackStackAware<CreatePaymentAccountPresenter>()
    val uiState by presenter.uiState.collectAsState()

    val flowNavController = rememberNavController()
    val navigationManager: NavigationManager = koinInject()
    val backStackEntry by flowNavController.currentBackStackEntryAsState()

    LaunchedEffect(presenter) {
        presenter.effect.collect { effect ->
            when (effect) {
                is CreatePaymentAccountEffect.NavigateToPaymentAccountForm -> {
                    flowNavController.navigate(CreatePaymentAccountRoute.PaymentAccountForm)
                }

                is CreatePaymentAccountEffect.NavigateToPaymentAccountReview -> {
                    flowNavController.navigate(CreatePaymentAccountRoute.PaymentAccountReview)
                }
            }
        }
    }

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
            paymentAccount = uiState.paymentAccount,
            accountType = accountType,
            paymentMethod = uiState.paymentMethod,
            onNavigateFromSelectPaymentMethod = { paymentMethod ->
                presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromSelectPaymentMethod(paymentMethod))
            },
            onNavigateFromPaymentAccountForm = { paymentAccount ->
                presenter.onAction(CreatePaymentAccountUiAction.OnNavigateFromPaymentAccountForm(paymentAccount))
            },
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
        when (accountType) {
            PaymentAccountType.FIAT -> "mobile.user.paymentAccounts.fiat.add".i18n()
            PaymentAccountType.CRYPTO -> "mobile.user.paymentAccounts.crypto.add".i18n()
        }

    return CreatePaymentAccountTopBarState(
        title = title,
        showCloseIcon = isFormOrReview,
    )
}
