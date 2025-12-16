package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import kotlinx.coroutines.launch
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WebLinkConfirmationDialog
import network.bisq.mobile.presentation.common.ui.utils.toClipEntry
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

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
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showConfirmDialog by remember { mutableStateOf(false) }

    val annotatedString = buildAnnotatedString {
        append(notes)
        append(" ")

        if (uri != null) {
            withLink(
                LinkAnnotation.Url(
                    url = uri,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = BisqTheme.colors.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ),
                    linkInteractionListener = { _ ->
                        // Handle URL click with confirmation if needed
                        if (openConfirmation) {
                            showConfirmDialog = true
                        } else {
                            uriHandler.openUri(uri)
                        }
                    }
                )
            ) {
                append(linkText)
            }
        } else if (onLinkClick != null) {
            // For custom actions, use LinkAnnotation.Clickable
            withLink(
                LinkAnnotation.Clickable(
                    tag = "custom_action",
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = BisqTheme.colors.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                ) {
                    // Handle custom click with confirmation if needed
                    if (openConfirmation) {
                        showConfirmDialog = true
                    } else {
                        onLinkClick()
                    }
                }
            ) {
                // Apply styling manually since LinkAnnotation.Clickable doesn't have styles parameter
                val start = length
                append(linkText)
                val end = length
                addStyle(
                    SpanStyle(
                        color = BisqTheme.colors.primary,
                        textDecoration = TextDecoration.Underline
                    ), start, end
                )
            }
        } else {
            // Fallback: just styled text without link functionality
            val start = length
            append(linkText)
            val end = length
            addStyle(
                SpanStyle(
                    color = BisqTheme.colors.primary,
                    textDecoration = TextDecoration.Underline
                ), start, end
            )
        }
    }

    BasicText(
        text = annotatedString,
        style = BisqTheme.typography.smallRegular.copy(
            color = BisqTheme.colors.mid_grey20,
            textAlign = textAlign
        )
    )

    if (showConfirmDialog) {
        WebLinkConfirmationDialog(
            link = uri ?: linkText,
            onConfirm = {
                if (onLinkClick != null) {
                    onLinkClick()
                } else if (uri != null) {
                    uriHandler.openUri(uri)
                }
                showConfirmDialog = false
            },
            onDismiss = {
                scope.launch {
                    clipboard.setClipEntry(AnnotatedString(uri ?: linkText).toClipEntry())
                }
                showConfirmDialog = false
            }
        )
    }
}