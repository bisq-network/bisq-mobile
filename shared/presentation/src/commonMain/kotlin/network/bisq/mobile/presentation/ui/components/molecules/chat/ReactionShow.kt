package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage

@Composable
fun ChatReactionShow(
    reaction: String,
    modifier: Modifier = Modifier,
) {
    // TODO: Should handle list of ReactionType
    // TODO: On click, show all reaction with user names in BottomModal (whatsapp ref) with option to remove self-reaction
    Box(modifier = modifier) {
        DynamicImage(
            reaction,
            modifier = Modifier.size(16.dp)
        )
    }

}