package network.bisq.mobile.presentation.create_payment_account.payment_account_form.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.openUriSafely

private val HyperlinkTokenRegex = Regex("\\[HYPERLINK:\\s*([^]]+)]")

@ExcludeFromCoverage
@Composable
fun PaymentMethodBackgroundInformationDialog(
    bodyText: String,
    onDismissRequest: () -> Unit,
    onError: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey40)
                    .padding(BisqUIConstants.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BisqText.H6Regular("paymentAccounts.createAccount.accountData.backgroundOverlay.headline".i18n())
            BisqGap.V1()
            Box(
                modifier =
                    Modifier
                        .weight(1f, false)
                        .verticalScroll(rememberScrollState()),
            ) {
                LinkifiedBodyText(bodyText = bodyText, onError = onError)
            }
            BisqGap.V1()
            BisqButton(
                text = "action.iUnderstand".i18n(),
                onClick = onDismissRequest,
                padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding, vertical = 8.dp),
                fullWidth = true,
            )
        }
    }
}

@ExcludeFromCoverage
@Composable
private fun LinkifiedBodyText(
    bodyText: String,
    onError: (() -> Unit)?,
) {
    val uriHandler = LocalUriHandler.current
    val text =
        buildAnnotatedString {
            var lastIndex = 0

            HyperlinkTokenRegex.findAll(bodyText).forEach { match ->
                val start = match.range.first
                val endExclusive = match.range.last + 1
                val url =
                    match.groupValues
                        .getOrNull(1)
                        ?.trim()
                        .orEmpty()

                if (lastIndex < start) {
                    append(bodyText.substring(lastIndex, start))
                }

                if (url.isNotBlank()) {
                    withLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles =
                                TextLinkStyles(
                                    style =
                                        SpanStyle(
                                            color = BisqTheme.colors.primary,
                                            textDecoration = TextDecoration.Underline,
                                        ),
                                ),
                            linkInteractionListener = {
                                paymentMethodBackgroundHyperlinkInteraction(uriHandler, url, onError)
                            },
                        ),
                    ) {
                        append(url)
                    }
                } else {
                    append(match.value)
                }

                lastIndex = endExclusive
            }

            if (lastIndex < bodyText.length) {
                append(bodyText.substring(lastIndex))
            }
        }

    BasicText(
        text = text,
        style =
            BisqTheme.typography.baseLight.copy(
                color = BisqTheme.colors.white,
            ),
    )
}

internal fun paymentMethodBackgroundHyperlinkInteraction(
    uriHandler: UriHandler,
    url: String,
    onError: (() -> Unit)? = null,
): Boolean = openPaymentMethodBackgroundInfoUri(uriHandler, url, onError)

internal fun openPaymentMethodBackgroundInfoUri(
    uriHandler: UriHandler,
    url: String,
    onError: (() -> Unit)? = null,
): Boolean {
    val result = uriHandler.openUriSafely(url)
    if (!result) {
        onError?.invoke()
    }
    return result
}

@Preview
@Composable
private fun PaymentMethodBackgroundInformationDialogPreview() {
    BisqTheme.Preview {
        PaymentMethodBackgroundInformationDialog(
            bodyText =
                "Zelle is a bank-to-bank payment method in the US. " +
                    "Use an account name that helps you identify this payment account later.",
            onDismissRequest = {},
        )
    }
}

@Preview
@Composable
private fun PaymentMethodBackgroundInformationDialogPreview_LongBodyPreview() {
    BisqTheme.Preview {
        PaymentMethodBackgroundInformationDialog(
            bodyText =
                "Zelle is a money transfer service that works best *through* your bank.\\n\\n\\\n" +
                    "  1. Check whether (and how) your bank supports Zelle: [HYPERLINK:https://www.zellepay.com/get-started]\\n\\\n" +
                    "  2. Pay close attention to transfer limitsâ\u0080\u0094banks often set separate daily, weekly, and monthly sending caps.\\n\\\n" +
                    "  3. If your bank does not support Zelle, you can still use the Zelle mobile app, but limits will be much lower.\\n\\\n" +
                    "  4. The name on your Bisq account **must** match the name on your Zelle/bank account.\\n\\n\\\n" +
                    "  If you cannot complete the Zelle payment exactly as specified in the trade contract, you may lose part or all of your security deposit.\\n\\n\\\n" +
                    "  Because Zelle carries a higher chargeback risk, sellers are advised to contact new buyers via email or SMS to confirm they own the account.",
            onDismissRequest = {},
        )
    }
}
