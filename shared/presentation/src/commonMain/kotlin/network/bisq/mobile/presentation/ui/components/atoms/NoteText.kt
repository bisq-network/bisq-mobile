package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import network.bisq.mobile.presentation.ui.theme.BisqTheme

// Pass either uri or onLinkClick. Not both
@Composable
fun NoteText(
    notes: String,
    linkText: String,
    uri: String? = null,
    textAlign: TextAlign = TextAlign.Start,
    onLinkClick: (() -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current

    val annotatedString = buildAnnotatedString {
        append(notes)
        append(" ")

        val start = length
        append(linkText)
        val end = length

        addStyle(
            SpanStyle(
                color = BisqTheme.colors.primary,
                textDecoration = TextDecoration.Underline
            ), start, end
        )

        // Add a custom annotation for click handling
        addStringAnnotation(
            tag = "LINK",
            annotation = uri ?: "custom_action", // Use "custom_action" if no URI
            start = start,
            end = end
        )
    }

    // TODO: ClickableText is deprecated. Hard to do this with Text/BasicText
    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "LINK", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    if (onLinkClick != null && annotation.item == "custom_action") {
                        onLinkClick()
                    } else if (uri != null) {
                        uriHandler.openUri(uri)
                    }
                }
        },
        style = androidx.compose.ui.text.TextStyle(
            color = BisqTheme.colors.grey2,
            fontSize = FontSize.SMALL.size,
            textAlign = textAlign
        )
    )
}