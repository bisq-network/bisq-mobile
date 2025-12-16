package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandler(onBackPressed: () -> Unit)