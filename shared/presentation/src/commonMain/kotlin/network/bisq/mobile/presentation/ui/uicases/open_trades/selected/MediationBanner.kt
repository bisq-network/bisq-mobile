package network.bisq.mobile.presentation.ui.uicases.open_trades.selected

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIconGrey
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun MediationBanner() {
    Column(
        modifier = Modifier.fillMaxSize()
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(12.dp))
            .background(color = BisqTheme.colors.yellow)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            WarningIconGrey(modifier = Modifier.size(20.dp).offset(y = 2.dp))
            BisqText.baseRegular(
                text = "mobile.openTrades.inMediation.banner".i18n(),
                color = BisqTheme.colors.dark_grey50
            )
        }
    }
}