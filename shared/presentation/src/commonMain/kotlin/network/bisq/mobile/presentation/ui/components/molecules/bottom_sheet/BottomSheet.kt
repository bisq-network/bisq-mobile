package network.bisq.mobile.presentation.ui.components.molecules.bottom_sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BisqBottomSheet(
    onDismissRequest:() -> Unit,
    containerColor: Color = BisqTheme.colors.dark_grey40,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(16.dp),
        sheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
        containerColor = containerColor,
        dragHandle = {
            Box(
                modifier = Modifier.padding(top = 20.dp)
                    .clip(shape = RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier.height(4.dp).width(60.dp)
                        .background(Color(0xFF6F6F6F))
                )
            }
        }
    ) {
        content()
    }
}

