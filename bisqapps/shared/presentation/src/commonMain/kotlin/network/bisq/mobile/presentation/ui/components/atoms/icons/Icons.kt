package network.bisq.mobile.presentation.ui.components.atoms.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.*
import org.jetbrains.compose.resources.painterResource

@Composable
fun BellIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_bell), "Bell icon", modifier = modifier)
}

@Composable
fun CopyIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_copy), "Copy icon", modifier = modifier)
}

@Composable
fun QuestionIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_question_mark), "Question icon", modifier = modifier)
}

@Composable
fun ScanIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_qr), "Scan icon", modifier = modifier)
}

@Composable
fun SortIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.icon_sort), "Sort icon", modifier = modifier)
}

@Composable
fun UserIcon(modifier: Modifier = Modifier) {
    Image(painterResource(Res.drawable.img_bot_image), "User icon", modifier = modifier)
}