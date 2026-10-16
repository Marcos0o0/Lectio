package app.lectio.reader

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Native accessibility controls and Chromium, independent of Espresso's WebView idling. */
@RunWith(AndroidJUnit4::class)
class ReaderDeviceTest {
    @get:Rule val activity=ActivityScenarioRule(MainActivity::class.java)
    private val instrumentation get()=InstrumentationRegistry.getInstrumentation()
    private val automation get()=instrumentation.uiAutomation

    private fun await(description: String, condition: () -> Boolean) {
        val deadline=SystemClock.elapsedRealtime()+30_000
        do {
            if(condition()) return
            SystemClock.sleep(100)
        } while(SystemClock.elapsedRealtime()<deadline)
        fail("No se cumplió en 30 s: $description")
    }

    private fun findNode(root: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        root ?: return null
        if(predicate(root)) return root
        for(i in 0 until root.childCount) findNode(root.getChild(i),predicate)?.let {return it}
        return null
    }

    private fun textNode(text: String)=findNode(automation.rootInActiveWindow) {it.text?.toString()==text}
    private fun tap(text: String? = null, description: String? = null) {
        val label=text ?: description.orEmpty()
        await("Control activado: $label") {
            var target=findNode(automation.rootInActiveWindow) {
                if(text!=null) it.text?.toString()==text else it.contentDescription?.toString()==description
            }
            while(target!=null&&!target.isClickable) target=target.parent
            target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true
        }
    }

    private fun chooseTheme(theme: String) {
        tap(text=theme)
        await("Tema $theme seleccionado") {
            var node=textNode(theme)
            var selected=false
            while(node!=null) {if(node.isSelected||node.isChecked) selected=true;node=node.parent}
            selected
        }
    }

    private fun findWebView(view: View): WebView? {
        if(view is WebView) return view
        if(view is ViewGroup) for(i in 0 until view.childCount) findWebView(view.getChildAt(i))?.let {return it}
        return null
    }

    private fun javascript(script: String): String {
        val value=AtomicReference<String>()
        val done=CountDownLatch(1)
        activity.scenario.onActivity { host ->
            val webView=requireNotNull(findWebView(host.window.decorView)) {"El lector no creó un WebView"}
            webView.evaluateJavascript(script) {value.set(it);done.countDown()}
        }
        assertTrue("WebView no respondió al JavaScript",done.await(10,TimeUnit.SECONDS))
        return value.get()
    }

    private fun library() {
        await("Biblioteca visible") {textNode("Un libro.\nOtro mundo.")!=null}
    }
    private fun openBook() {
        tap(text="A Little More Slowly")
        await("Lector visible") {findNode(automation.rootInActiveWindow) {it.contentDescription=="Volver a la biblioteca"}!=null}
    }
    private fun waitForChapter(fragment: String) {
        await("Contenido del capítulo: $fragment") {javascript("document.body && document.body.innerText.includes(${org.json.JSONObject.quote(fragment)})")=="true"}
    }
    private fun capture(name: String) {
        SystemClock.sleep(250)
        val directory=File(instrumentation.targetContext.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        val bitmap=requireNotNull(automation.takeScreenshot())
        File(directory,"$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }

    @Test fun bookContentSelectionAndReopeningUseRealWebView() {
        library()
        tap(text="Ajustes");chooseTheme("Oscuro");tap(text="Biblioteca")
        library();capture("phone-library-dark-android15")
        openBook()
        tap(description="Índice y anotaciones")
        await("Índice abierto") {textNode("Dentro de este libro")!=null}
        tap(text="The quiet between things")
        await("Índice cerrado") {textNode("Dentro de este libro")==null}
        waitForChapter("For a few precious minutes")
        assertEquals("\"rgb(16, 18, 22)\"",javascript("getComputedStyle(document.documentElement).backgroundColor"))
        capture("phone-reader-dark-android15")
        assertEquals("true",javascript("""(function(){const n=document.querySelector('p').firstChild;const i=n.textContent.indexOf('precious');if(i<0)return false;const r=document.createRange();r.setStart(n,i);r.setEnd(n,i+8);const s=getSelection();s.removeAllRanges();s.addRange(r);document.dispatchEvent(new Event('selectionchange'));return true})()"""))
        await("Traducción nativa") {textNode("precioso/a; valioso/a")!=null}
        capture("phone-lookup-dark-android15")
        tap(description="Cerrar consulta")
        tap(description="Capítulo siguiente")
        waitForChapter("On my desk there is a notebook")
        tap(description="Guardar marcador")
        tap(description="Volver a la biblioteca")
        library();openBook()
        waitForChapter("On my desk there is a notebook")
        tap(description="Volver a la biblioteca");library()
    }

    @Test fun lightAndDarkPreferencesReachTheReadingPage() {
        library();tap(text="Ajustes")
        chooseTheme("Claro");capture("phone-settings-light-android15")
        chooseTheme("Oscuro");capture("phone-settings-dark-android15")
        chooseTheme("Claro");tap(text="Biblioteca");openBook()
        await("Lector claro") {javascript("getComputedStyle(document.documentElement).backgroundColor")=="\"rgb(246, 248, 252)\""}
        capture("phone-reader-light-android15")
        tap(description="Volver a la biblioteca")
        library();tap(text="Ajustes");chooseTheme("Oscuro")
    }
}
