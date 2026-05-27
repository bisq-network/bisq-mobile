package network.bisq.mobile.client.payment_accounts.presentation.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import network.bisq.mobile.domain.utils.EMPTY_STRING
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.GreyCloseButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@Composable
fun AccountFlowDialog(
    title: String,
    bodyText: String,
    onDismissRequest: () -> Unit,
) {
    AccountFlowDialogContent(
        title = title,
        bodyText = bodyText,
        initialSearchQuery = EMPTY_STRING,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun AccountFlowDialogContent(
    title: String,
    bodyText: String,
    initialSearchQuery: String,
    onDismissRequest: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf(initialSearchQuery) }

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H6Regular(title)
            BisqGap.V1()
            BisqSearchField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "action.search".i18n(),
            )
            BisqGap.V1()
            Box(
                modifier =
                    Modifier
                        .weight(1f, false)
                        .verticalScroll(rememberScrollState()),
            ) {
                BisqText.StyledText(
                    text =
                        highlightedBodyText(
                            bodyText = bodyText,
                            searchQuery = searchQuery,
                            highlightStyle =
                                SpanStyle(
                                    color = BisqTheme.colors.backgroundColor,
                                    background = BisqTheme.colors.warning,
                                ),
                        ),
                    style = BisqTheme.typography.baseLight,
                )
            }
            BisqGap.V1()
            GreyCloseButton(onClick = onDismissRequest)
        }
    }
}

private fun highlightedBodyText(
    bodyText: String,
    searchQuery: String,
    highlightStyle: SpanStyle,
): AnnotatedString {
    if (searchQuery.isBlank()) {
        return AnnotatedString(bodyText)
    }

    return buildAnnotatedString {
        append(bodyText)

        var startIndex = 0
        while (startIndex < bodyText.length) {
            val matchIndex =
                bodyText.indexOf(
                    string = searchQuery,
                    startIndex = startIndex,
                    ignoreCase = true,
                )

            if (matchIndex == -1) {
                break
            }

            val matchEnd = matchIndex + searchQuery.length
            addStyle(
                style = highlightStyle,
                start = matchIndex,
                end = matchEnd,
            )
            startIndex = matchEnd
        }
    }
}

@Preview
@Composable
private fun AccountFlowDialogPreview() {
    BisqTheme.Preview {
        AccountFlowDialogContent(
            title = "Supported currencies",
            bodyText = previewBodyText(),
            initialSearchQuery = "",
            onDismissRequest = {},
        )
    }
}

@Preview
@Composable
private fun AccountFlowDialogHighlightedPreview() {
    BisqTheme.Preview {
        AccountFlowDialogContent(
            title = "Supported currencies",
            bodyText = previewBodyText(),
            initialSearchQuery = "dollar",
            onDismissRequest = {},
        )
    }
}

@ExcludeFromCoverage
fun previewBodyText() = "USD (US Dollar), EUR (Euro), CAD (Canadian Dollar), AUD (Australian Dollar), GBP (British Pound)"
