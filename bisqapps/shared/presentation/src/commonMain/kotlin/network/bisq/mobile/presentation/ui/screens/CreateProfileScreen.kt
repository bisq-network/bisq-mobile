package network.bisq.mobile.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import bisqapps.shared.presentation.generated.resources.Res
import coil3.compose.AsyncImage
import network.bisq.mobile.components.MaterialTextField
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.*
import org.jetbrains.compose.resources.ExperimentalResourceApi

private lateinit var textState: MutableState<String>

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CreateProfileScreen(
    rootNavController: NavController
) {
    textState = remember { mutableStateOf("") }
    Scaffold(
        containerColor = backgroundColor,
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = Res.getUri("drawable/logo_with_slogan.svg"),
                    contentDescription = null,
                    modifier = Modifier.height(62.dp).width(200.dp),
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Create your profile",
                    fontSize = 36.sp,
                    color = grey1
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Your public profile consists of a nickname (picked by you) and bot icon (generated cryptographically)",
                    color = grey2,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(40.dp))
                Column(modifier = Modifier.padding(horizontal = 54.dp)) {
                    Text(text = "Profile nickname", color = White2)
                    MaterialTextField(textState.value, onValueChanged = { textState.value = it })
                }

                Spacer(modifier = Modifier.height(36.dp))
                AsyncImage(
                    model = Res.getUri("drawable/bot_image.svg"),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Sleepily-Distracted-Zyophyte-257",
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "BOT ID",
                    fontSize = 18.sp,
                    color = grey2
                )
                Spacer(modifier = Modifier.height(38.dp))
                Text(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(8.dp))
                        .background(color = Black5)
                        .clickable(
                            indication = null,
                            interactionSource = remember {
                                MutableInteractionSource()
                            },
                            onClick = {})
                        .padding(horizontal = 64.dp, vertical = 12.dp),
                    text = "Generate new bot icon",
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    modifier = Modifier
                        .clip(shape = RoundedCornerShape(8.dp))
                        .background(color = if (textState.value.isEmpty()) primaryDisabled else primaryStandard)
                        .clickable(
                            indication = null,
                            interactionSource = remember {
                                MutableInteractionSource()
                            },
                            onClick = {
                                if (textState.value.isNotEmpty()) {
                                    rootNavController.navigate(Routes.BisqUrl.name) {
                                        popUpTo(Routes.CreateProfile.name) {
                                            inclusive = true
                                        }
                                    }
                                }
                            })
                        .padding(horizontal = 64.dp, vertical = 12.dp),
                    text = "Next",
                    color = if (textState.value.isEmpty()) grey2 else Color.White
                )
            }
        }
    }
}