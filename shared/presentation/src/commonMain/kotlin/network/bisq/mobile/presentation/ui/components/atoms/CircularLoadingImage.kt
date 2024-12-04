package network.bisq.mobile.presentation.ui.components.atoms

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun CircularLoadingImage(
    image: DrawableResource,
    isLoading: Boolean
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(image), "",
            modifier = Modifier.height(36.dp).width(30.dp)
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp),
                color = BisqTheme.colors.primaryDisabled,
                strokeWidth = 2.dp
            )
        }
    }
}