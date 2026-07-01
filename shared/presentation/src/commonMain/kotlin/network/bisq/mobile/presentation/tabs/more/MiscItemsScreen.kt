package network.bisq.mobile.presentation.tabs.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_question_mark
import bisqapps.shared.presentation.generated.resources.nav_accounts
import bisqapps.shared.presentation.generated.resources.nav_ignored_users
import bisqapps.shared.presentation.generated.resources.nav_reputation
import bisqapps.shared.presentation.generated.resources.nav_resources
import bisqapps.shared.presentation.generated.resources.nav_settings
import bisqapps.shared.presentation.generated.resources.nav_support
import bisqapps.shared.presentation.generated.resources.nav_user
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun MiscItemsScreen() {
    val presenter: MiscItemsPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.uiState.collectAsState()

    MiscItemsContent(uiState = uiState, onAction = presenter::onAction)
}

@Composable
internal fun MiscItemsContent(
    uiState: MiscItemsUiState,
    onAction: (MiscItemsUiAction) -> Unit,
) {
    BisqScaffold {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
        ) {
            uiState.sections.forEach { section ->
                SectionHeader(title = section.title.i18n())
                section.items.forEach { item ->
                    ItemButton(item = item) { onAction(MiscItemsUiAction.OnMenuItemClick(item.route)) }
                    BisqGap.VHalf()
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val modifier =
        Modifier
            .fillMaxWidth()
            .padding(
                start = BisqUIConstants.ScreenPadding,
                top = BisqUIConstants.ScreenPadding,
                bottom = BisqUIConstants.ScreenPaddingHalf,
            )
    BisqText.XSmallMedium(
        text = title.uppercase(),
        color = BisqTheme.colors.mid_grey20,
        modifier = modifier,
    )
}

@Composable
private fun ItemButton(
    item: MenuItem,
    onClick: () -> Unit,
) {
    val label = item.label.i18n()
    val iconTint = if (item.isEnabled) null else ColorFilter.tint(BisqTheme.colors.mid_grey20)
    BisqButton(
        text = label,
        onClick = onClick,
        fullWidth = true,
        backgroundColor = BisqTheme.colors.dark_grey40,
        disabled = !item.isEnabled,
        leftIcon = {
            Image(
                painter = painterResource(item.icon),
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                colorFilter = iconTint,
            )
        },
        rightIcon = { ArrowRightIcon() },
        textAlign = TextAlign.Start,
        padding = PaddingValues(all = BisqUIConstants.ScreenPadding),
    )
}

private val miscItemsPreviewState =
    MiscItemsUiState(
        sections =
            listOf(
                MenuSection(
                    title = UiString("mobile.more.section.identity"),
                    items =
                        listOf(
                            MenuItem(UiString("mobile.more.userProfile"), Res.drawable.nav_user, NavRoute.UserProfile),
                            MenuItem(
                                label = UiString("mobile.settings.ignoredUsers"),
                                icon = Res.drawable.nav_ignored_users,
                                route = NavRoute.IgnoredUsers,
                                isEnabled = false,
                            ),
                            MenuItem(
                                UiString("mobile.more.reputation"),
                                Res.drawable.nav_reputation,
                                NavRoute.Reputation,
                            ),
                        ),
                ),
                MenuSection(
                    title = UiString("mobile.more.section.tradingSetup"),
                    items =
                        listOf(
                            MenuItem(
                                UiString("mobile.more.paymentAccounts"),
                                Res.drawable.nav_accounts,
                                NavRoute.PaymentAccounts,
                            ),
                        ),
                ),
                MenuSection(
                    title = UiString("mobile.more.section.help"),
                    items =
                        listOf(
                            MenuItem(UiString("mobile.more.support"), Res.drawable.nav_support, NavRoute.Support),
                            MenuItem(UiString("mobile.more.faqs"), Res.drawable.icon_question_mark, NavRoute.Faqs),
                        ),
                ),
                MenuSection(
                    title = UiString("mobile.more.section.app"),
                    items =
                        listOf(
                            MenuItem(UiString("mobile.more.settings"), Res.drawable.nav_settings, NavRoute.Settings),
                            MenuItem(UiString("mobile.more.resources"), Res.drawable.nav_resources, NavRoute.Resources),
                        ),
                ),
            ),
    )

@ExcludeFromCoverage
@Preview(name = "More — sections (Ignored users disabled)")
@Composable
private fun MiscItemsContentPreview() {
    BisqTheme.Preview {
        MiscItemsContent(uiState = miscItemsPreviewState, onAction = {})
    }
}
