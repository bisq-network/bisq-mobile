package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqText.styledText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.offer.SellerReputationWarningDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferDirectionScreen() {
    val presenter: CreateOfferDirectionPresenter = koinInject()
    val createPresenter: CreateOfferPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val showSellerReputationWarning by presenter.showSellerReputationWarning.collectAsState()

    MultiScreenWizardScaffold(
        "bisqEasy.tradeWizard.progress.directionAndMarket".i18n(),
        stepIndex = 1,
        stepsLength = if (createPresenter.skipCurrency) 6 else 7,
        horizontalAlignment = Alignment.Start,
        showNextPrevButtons = false,
        shouldBlurBg = showSellerReputationWarning,
        showUserAvatar = false,
        closeAction = !showSellerReputationWarning,
        onConfirmedClose = presenter::onClose
    ) {
        if (presenter.marketName != null) {
            val full = presenter.headline
            val name = presenter.marketName!!
            val start = full.indexOf(name)
            if (start >= 0) {
                val annotated = buildAnnotatedString {
                    append(full)
                    addStyle(SpanStyle(color = BisqTheme.colors.primary), start, start + name.length)
                }
                styledText(text = annotated,
                    style = BisqTheme.typography.h3Light,
                    autoResize = true,
                    maxLines = 2)
            } else {
                AutoResizeText(
                    full,
                    color = BisqTheme.colors.white,
                    textStyle = BisqTheme.typography.h3Light,
                    maxLines = 2,
                )
            }
        } else {
            AutoResizeText(
                presenter.headline,
                color = BisqTheme.colors.white,
                textStyle = BisqTheme.typography.h3Light,
                maxLines = 1,
            )
        }

        BisqGap.V2()

        BisqButton(
            onClick = { presenter.onBuySelected() },
            backgroundColor = BisqTheme.colors.primaryDim,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = BisqUIConstants.ScreenPadding4X),
            textComponent = { BisqText.h4Light("bisqEasy.tradeWizard.directionAndMarket.buy".i18n()) }
        )
        BisqGap.VHalf()
        BisqText.largeLightGrey("mobile.bisqEasy.tradeWizard.direction.buy.helpText".i18n(), textAlign = TextAlign.Center)

        BisqGap.V2()

        BisqButton(
            onClick = { presenter.onSellSelected() },
            backgroundColor = BisqTheme.colors.dark_grey40,
            modifier = Modifier.fillMaxWidth(),
            padding = PaddingValues(vertical = BisqUIConstants.ScreenPadding4X),
            textComponent = { BisqText.h4Light("bisqEasy.tradeWizard.directionAndMarket.sell".i18n()) }
        )
        BisqGap.VHalf()
        BisqText.largeLightGrey("mobile.bisqEasy.tradeWizard.direction.sell.helpText".i18n(), textAlign = TextAlign.Center)
    }

    if (showSellerReputationWarning) {
        SellerReputationWarningDialog(
            onDismiss = { presenter.onDismissSellerReputationWarning() },
            onClick = { presenter.onNavigateToReputation() },
        )
    }
}