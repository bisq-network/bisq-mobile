package bisq.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import bisq.android.main.MainController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import bisq.android.ui.theme.MobileTheme
import lombok.extern.slf4j.Slf4j


@Slf4j
class MainActivity : ComponentActivity() {
    var logViewModel = LogViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userDataDir = getFilesDir().toPath()
        var controller = MainController(userDataDir)

        // Map observable log message from java model to logViewModel
        controller.getLogMessage().addObserver { info ->
            logViewModel.setLogMessage(info)
        }
        controller.initialize()

        enableEdgeToEdge()
        setContent {
            MobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LogView(
                        logViewModel = logViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

class LogViewModel : ViewModel() {
    private val _logMessage = MutableStateFlow("N/A")
    val logMessage: StateFlow<String> = _logMessage
    fun setLogMessage(newValue: String) {
        viewModelScope.launch {
            _logMessage.value = newValue
        }
    }
}

@Composable
fun LogView(logViewModel: LogViewModel, modifier: Modifier = Modifier) {
    val value by logViewModel.logMessage.collectAsState()
    Text(
        text = value,
        modifier = modifier
    )
}
