import androidx.compose.ui.window.ComposeUIViewController
import org.company.app.App
import platform.UIKit.UIViewController

@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
