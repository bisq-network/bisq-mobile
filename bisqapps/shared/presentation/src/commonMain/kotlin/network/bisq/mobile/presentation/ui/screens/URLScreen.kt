package network.bisq.mobile.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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
fun URLScreen(
    rootNavController: NavController
) {
    textState = remember { mutableStateOf("") }
    val isConnected by remember { mutableStateOf(false) }
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
                    model = Res.getUri("drawable/log_with_slogan.svg"),
                    contentDescription = null,
                    modifier = Modifier.height(62.dp).width(200.dp),
                )
                Spacer(modifier = Modifier.height(32.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bisq URL",
                            fontSize = 28.sp,
                            color = Color.White
                        )
                        AsyncImage(
                            model = Res.getUri("drawable/question_mark.svg"),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    MaterialTextField(textState.value, onValueChanged = { textState.value = it })

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(shape = RoundedCornerShape(8.dp))
                                .background(color = Black5)
                                .padding(horizontal = 46.dp, vertical = 12.dp)

                        ) {
                            AsyncImage(
                                model = Res.getUri("drawable/copy.svg"),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Paste",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(shape = RoundedCornerShape(8.dp))
                                .background(color = primaryStandard)
                                .padding(horizontal = 46.dp, vertical = 12.dp)
                        ) {
                            AsyncImage(
                                model = Res.getUri("drawable/qr.svg"),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Scan",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(36.dp))
                    Text(
                        text = "STATUS",
                        fontSize = 18.sp,
                        color = grey2
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if(isConnected) "Connected" else "Not Connected",
                            fontSize = 22.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "",
                            modifier = Modifier.clip(
                                RoundedCornerShape(5.dp)
                            ).background(color =if(isConnected) primaryStandard else Danger1).size(10.dp),
                        )
                    }

                }

            }
            var visible by remember {
                mutableStateOf(false)
            }


            if (!visible) {
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
                                visible = !visible
                            })
                        .padding(horizontal = 32.dp, vertical = 12.dp),
                    text = "Test Connection",
                    color = if (textState.value.isEmpty()) grey2 else Color.White,
                )
            } else {
                Row (modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)){
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(700)),
                        ) {
                        Text(
                            modifier = Modifier

                                .clip(shape = RoundedCornerShape(8.dp))
                                .background(color = Black5)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember {
                                        MutableInteractionSource()
                                    },
                                    onClick = {

                                    })
                                .padding(horizontal = 32.dp, vertical = 12.dp),
                            text = "Test Connection",
                            color = if (textState.value.isEmpty()) grey2 else Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(300)),

                        ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape = RoundedCornerShape(8.dp))
                                .background(color = primaryStandard)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember {
                                        MutableInteractionSource()
                                    },
                                    onClick = {
                                        rootNavController.navigate(Routes.TabContainer.name) {
                                            popUpTo(Routes.BisqUrl.name) {
                                                inclusive = true
                                            }
                                        }
                                    })
                                .padding(horizontal = 32.dp, vertical = 12.dp),
                            text = "Next",
                            textAlign = TextAlign.Center,
                            color = Color.White,
                        )
                    }
                }

            }


        }

    }
}
