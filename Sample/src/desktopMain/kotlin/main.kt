import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.company.app.App

fun main()= application {
    System.setProperty("compose.interop.blending", "true")
    System.setProperty("compose.swing.render.on.layer", "true")
    System.setProperty("compose.interop.blending", "true")

    Window(
        title = "CameraK",
        state = rememberWindowState(width = 1440.dp, height = 1024.dp),
        onCloseRequest = ::exitApplication,
    ) {
        App()
    }

}