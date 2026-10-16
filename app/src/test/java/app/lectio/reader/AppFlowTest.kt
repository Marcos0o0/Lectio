package app.lectio.reader

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import android.graphics.Bitmap

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],qualifiers="w412dp-h915dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppFlowTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        val target=File("build/test-screenshots/$name.png").apply {parentFile?.mkdirs()}
        // PixelCopy requires a real display surface. Draw Android's view tree with
        // Robolectric's native Canvas instead, keeping screenshot failures out of navigation.
        compose.runOnIdle {
            val root=compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(root.width,root.height,Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(bitmap))
            target.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun libraryNavigationAndReaderOpen() {
        compose.waitUntil(15000) {compose.onAllNodesWithText("¿Qué te gustaría leer?").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Importar EPUB",useUnmergedTree=true).assertExists()
        capture("library-dark")
        compose.onNodeWithText("Vocabulario").performClick()
        compose.onNodeWithText("Aún no hay palabras guardadas").assertExists()
        capture("vocabulary-empty")
        compose.onNodeWithText("Actividad").performClick()
        compose.onNodeWithText("Resumen").assertExists()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Claro").performClick()
        compose.waitUntil(5000) {compose.onAllNodes(hasText("Claro") and isSelected()).fetchSemanticsNodes().isNotEmpty()}
        capture("settings-light")
        compose.onNodeWithText("Oscuro").performClick()
        compose.waitUntil(5000) {compose.onAllNodes(hasText("Oscuro") and isSelected()).fetchSemanticsNodes().isNotEmpty()}
        compose.waitForIdle()
        capture("settings-dark")
        compose.onNodeWithText("Biblioteca").performClick()
        compose.onAllNodesWithText("A Little More Slowly")[0].performClick()
        compose.onNodeWithContentDescription("Volver a la biblioteca").assertExists()
        compose.onNodeWithContentDescription("Guardar marcador").performClick()
        capture("reader-marker")
        compose.onNodeWithContentDescription("Índice y anotaciones").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.waitUntil(10000) {compose.onAllNodesWithText("The quiet between things",useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("The quiet between things",useUnmergedTree=true).assertExists()
    }
}
