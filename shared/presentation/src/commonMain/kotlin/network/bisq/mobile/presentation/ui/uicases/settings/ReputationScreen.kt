package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.OrderedTextList
import network.bisq.mobile.presentation.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject


@Composable
fun ReputationScreen() {
    val presenter: ReputationPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val profileId by presenter.profileId.collectAsState()

    BisqScrollScaffold(
        topBar = { TopBar("mobile.more.reputation".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero),
        snackbarHostState = presenter.getSnackState(),
        isInteractive = isInteractive,
    ) {
        BisqText.baseLight(
            text = "mobile.reputation.info".i18n(),
            color = BisqTheme.colors.light_grey50,
        )
        BisqGap.V1()
        MaxTradeAmountFormula(
            formulaInput = "mobile.reputation.buildReputation.intro.part1.formula.input".i18n(),
            formulaOutput = "mobile.reputation.buildReputation.intro.part1.formula.output".i18n()
        )
        BisqGap.V1()
        BisqText.smallLight(
            text = "mobile.reputation.buildReputation.intro.part1.formula.footnote".i18n(),
            color = BisqTheme.colors.light_grey20,
        )

        BisqGap.V1()

        val part1 = "mobile.reputation.learnMore.part1".i18n()
        val part2 = "mobile.reputation.learnMore.part2".i18n()
        val fullText = "$part1 $part2"
        val annotatedString = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = BisqTheme.colors.light_grey50,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light
                )
            ) {
                append(part1)
                append(" ")
            }
            pushStringAnnotation(
                tag = "URL",
                annotation = BisqLinks.REPUTATION_WIKI_URL
            )
            withStyle(
                style = SpanStyle(
                    color = BisqTheme.colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Light,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(part2)
            }
            pop()
        }

        ClickableText(
            text = annotatedString,
            onClick = { offset ->
                annotatedString.getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let { annotation ->
                    presenter.onOpenWebUrl(annotation.item)
                }
            }
        )

        BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding, bottom = BisqUIConstants.ScreenPadding3X))

        BisqText.h2Light("reputation.buildReputation.title".i18n())
        BisqGap.V2()

        BisqCard {
            BisqText.h3Light("reputation.buildReputation.burnBsq.title".i18n())
            BisqGap.V1()

            BisqText.baseLight(
                "reputation.buildReputation.burnBsq.description".i18n(),
                color = BisqTheme.colors.light_grey50,
            )
            BisqGap.V2()

            BisqText.h4Light("mobile.reputation.burnedBsq.howToHeadline".i18n())
            BisqGap.V1()

            OrderedTextList(
                "mobile.reputation.burnedBsq.howTo".i18n(),
                regex = "- ",
                style = { text, modifier ->
                    BisqText.baseLight(
                        text = text,
                        modifier = modifier,
                        color = BisqTheme.colors.white,
                    )
                },
            )
            BisqGap.V2()
            BisqTextField(
                "reputation.pubKeyHash".i18n(),
                value = profileId,
                disabled = true,
                rightSuffix = { CopyIconButton(value = profileId) },
                backgroundColor = BisqTheme.colors.dark_grey30,
            )
            BisqGap.V1()
        }

        BisqGap.V2()

        BisqCard {
            BisqText.h3Light("reputation.buildReputation.bsqBond.title".i18n())
            BisqGap.V1()

            BisqText.baseLight(
                "reputation.buildReputation.bsqBond.description".i18n(),
                color = BisqTheme.colors.light_grey50,
            )
            BisqGap.V2()

            BisqText.h4Light("mobile.reputation.bond.howToHeadline".i18n())
            BisqGap.V1()

            OrderedTextList(
                "mobile.reputation.bond.howTo".i18n(),
                regex = "- ",
                style = { text, modifier ->
                    BisqText.baseLight(
                        text = text,
                        modifier = modifier,
                        color = BisqTheme.colors.white,
                    )
                },
            )
            BisqGap.V2()
            BisqTextField(
                "reputation.pubKeyHash".i18n(),
                value = profileId,
                disabled = true,
                rightSuffix = { CopyIconButton(value = profileId) },
                backgroundColor = BisqTheme.colors.dark_grey30,
            )
            // Give a bit extra space at bottom
            BisqGap.V1()
        }
    }
}

@Composable
fun MaxTradeAmountFormula(
    formulaOutput: String,
    formulaInput: String,
    formulaDivisor: String = "200",
    textColor: Color = BisqTheme.colors.white,
    textStyle: TextStyle = BisqTheme.typography.smallRegular,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .wrapContentWidth() // shrink row to its content
                .background(
                    color = BisqTheme.colors.dark_grey40,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(
                    vertical = BisqUIConstants.ScreenPadding,
                    horizontal = BisqUIConstants.ScreenPadding2X
                )
        ) {
            Text(text = formulaOutput, color = textColor, style = textStyle)

            Text(text = "=", color = textColor, style = textStyle)
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = formulaInput, color = textColor, style = textStyle)
                HorizontalDivider(
                    thickness = 1.dp,
                    color = textColor,
                    modifier = Modifier.width(120.dp)
                )
                Text(text = formulaDivisor, color = textColor, style = textStyle)
            }
        }
    }
}

