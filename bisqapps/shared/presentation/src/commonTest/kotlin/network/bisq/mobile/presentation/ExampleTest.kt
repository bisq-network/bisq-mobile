import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Example test for UI shared code on KMP Compose.
 * Not working- see https://slack-chats.kotlinlang.org/t/18784429/hi-there-i-m-trying-to-run-an-ui-test-in-shared-commontest-a
 * Ignoring till we figure out the problem bringing NPE on Build.FINGERPRINT (internal to compose ui test)
 */
@Ignore
class ExampleTest {

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun myTest() = runComposeUiTest {
        // Declares a mock UI to demonstrate API calls
        //
        // Replace with your own declarations to test the code of your project
        setContent {
            var text by remember { mutableStateOf("Hello") }
            Text(
                text = text,
                modifier = Modifier.testTag("text")
            )
            Button(
                onClick = { text = "Compose" },
                modifier = Modifier.testTag("button")
            ) {
                Text("Click me")
            }
        }

        // Tests the declared UI with assertions and actions of the Compose Multiplatform testing API
        onNodeWithTag("text").assertTextEquals("Hello")
        onNodeWithTag("button").performClick()
        onNodeWithTag("text").assertTextEquals("Compose")
    }
}