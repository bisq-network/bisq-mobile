package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WebLinkConfirmationDialog
import network.bisq.mobile.presentation.ui.theme.BisqTheme

// Pass either uri or onLinkClick. Not both
@Composable
fun NoteText(
    notes: String,
    linkText: String,
    uri: String? = null,
    textAlign: TextAlign = TextAlign.Start,
    openConfirmation: Boolean = false,
    onLinkClick: (() -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    var showConfirmDialog by remember { mutableStateOf(false) }

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
                        if (openConfirmation) {
                            showConfirmDialog = true
                        } else {
                            onLinkClick()
                        }
                    } else if (uri != null) {
                        if (openConfirmation) {
                            showConfirmDialog = true
                        } else {
                            uriHandler.openUri(uri)
                        }
                    }
                }
        },
        style = androidx.compose.ui.text.TextStyle(
            color = BisqTheme.colors.mid_grey20,
            fontSize = FontSize.SMALL.size,
            textAlign = textAlign
        )
    )

    if (showConfirmDialog) {
        WebLinkConfirmationDialog(
            link = linkText,
            onConfirm = {
                if (onLinkClick != null) {
                    onLinkClick()
                } else if (uri != null) {
                    uriHandler.openUri(uri)
                }
                showConfirmDialog = false
            },
            onDismiss = {
                clipboardManager.setText(buildAnnotatedString { append(linkText) })
                showConfirmDialog = false
            }
        )
    }
}