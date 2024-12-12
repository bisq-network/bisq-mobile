package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.ibm_plex_sans_regular
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.Font

@Composable
fun NoteText(
    notes: String,
    linkText: String,
    uri: String = "https://bisq.network/",
    textAlign: TextAlign = TextAlign.Start
) {
    val uriHandler = LocalUriHandler.current
    val annotatedString = buildAnnotatedString {
        append(notes)
        withStyle(
            style = SpanStyle(
                color = BisqTheme.colors.primary,
                textDecoration = TextDecoration.Underline
            )
        ) {
            val link =
                LinkAnnotation.Url(
                    uri,
                ) {
                    val url = (it as LinkAnnotation.Url).url
                    uriHandler.openUri(url)
                }
            withLink(link) { append(linkText) }
        }

    }
    Text(
        text = annotatedString,
        color = BisqTheme.colors.grey3,
        fontSize = 12.sp,
        textAlign = textAlign,
    )
}