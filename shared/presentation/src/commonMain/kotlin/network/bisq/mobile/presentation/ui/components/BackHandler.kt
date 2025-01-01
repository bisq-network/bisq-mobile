package network.bisq.mobile.presentation.ui.components

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandler(onBackPressed: () -> Unit)