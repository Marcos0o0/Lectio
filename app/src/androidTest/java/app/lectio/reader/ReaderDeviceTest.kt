package app.lectio.reader

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Before
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

    private fun await(description: String, timeout: Long = 30_000, condition: () -> Boolean) {
        val deadline=SystemClock.elapsedRealtime()+timeout
        do {
            if(condition()) return
            SystemClock.sleep(100)
        } while(SystemClock.elapsedRealtime()<deadline)
        fail("No se cumplió: $description")
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
    private fun scrollTo(text: String) {
        repeat(20) {
            if(textNode(text)?.isVisibleToUser==true) return
            val scroll=findNode(automation.rootInActiveWindow) {it.isScrollable}
            scroll?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            SystemClock.sleep(300)
        }
        fail("No se encontró $text")
    }

    private fun findWebView(view: View): WebView? {
        if(view is WebView) return view
        if(view is ViewGroup) for(i in 0 until view.childCount) findWebView(view.getChildAt(i))?.let {return it}
        return null
    }

    private fun javascript(script: String): String {
        await("WebView creado") {var exists=false;activity.scenario.onActivity {exists=findWebView(it.window.decorView)!=null};exists}
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
        await("Biblioteca visible") {textNode("¿Qué te gustaría leer?")!=null}
    }
    @Before fun resetReadingPreferences() {
        library()
        activity.scenario.onActivity {host -> androidx.lifecycle.ViewModelProvider(host)[ReaderViewModel::class.java].preferences {
            it.copy(theme="Oscuro",fontSize=20,lineHeight=1.8f,serif=true,readingTheme="Aplicación",margin=28,paragraphSpacing=1f,justified=false,keepScreenOn=false,tapToTranslate=true,showReadingHint=true,dictionaryEnabled=true)
        }}
        automation.waitForIdle(150,5000)
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
        val prefix=InstrumentationRegistry.getArguments().getString("capturePrefix")
        val filename=if(prefix!=null) name.replaceFirst("phone-","$prefix-") else name
        File(directory,"$filename.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }

    /** Find geometry only; the selection itself must come from real Android touch events. */
    private fun touchWord(word: String, longPress: Boolean) {
        await("Lector con foco táctil") {var focused=false;activity.scenario.onActivity {focused=it.hasWindowFocus()};focused}
        automation.waitForIdle(150,5000)
        val raw=javascript("""(function(){let n=document.querySelector('p').firstChild,i=n.textContent.indexOf(${org.json.JSONObject.quote(word)});let r=document.createRange();r.setStart(n,i);r.setEnd(n,i+${word.length});let b=r.getBoundingClientRect();return JSON.stringify({x:b.x+b.width/2,y:b.y+b.height/2,scale:devicePixelRatio})})()""")
        val position=org.json.JSONObject(org.json.JSONTokener(raw).nextValue() as String)
        val location=IntArray(2)
        activity.scenario.onActivity {host -> requireNotNull(findWebView(host.window.decorView)).getLocationOnScreen(location)}
        val x=location[0]+position.getDouble("x").toFloat()*position.getDouble("scale").toFloat()
        val y=location[1]+position.getDouble("y").toFloat()*position.getDouble("scale").toFloat()
        val down=SystemClock.uptimeMillis()
        fun event(action: Int) {
            val motion=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0).apply {source=InputDevice.SOURCE_TOUCHSCREEN}
            try {assertTrue("Android rechazó el gesto",automation.injectInputEvent(motion,true))} finally {motion.recycle()}
        }
        event(MotionEvent.ACTION_DOWN);SystemClock.sleep(if(longPress) 1000 else 70);event(MotionEvent.ACTION_UP)
    }
    private fun closeLookup() {
        tap(description="Cerrar consulta")
        await("Ficha cerrada") {findNode(automation.rootInActiveWindow) {it.contentDescription=="Cerrar consulta"}==null}
        await("Lector con foco") {var focused=false;activity.scenario.onActivity {focused=it.hasWindowFocus()};focused}
        automation.waitForIdle(150,5000)
        await("Selección retirada") {javascript("getSelection().toString()")=="\"\""}
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
        touchWord("precious",true)
        await("Ficha tras mantener pulsado") {textNode("precious")!=null}
        await("Traducción o descarga disponible") {textNode("Descargar por Wi-Fi")!=null||findNode(automation.rootInActiveWindow) {it.contentDescription=="Powered by Google Translate"}!=null}
        if(textNode("Descargar por Wi-Fi")!=null) {
            capture("phone-download-android15")
            tap(text="Descargar usando datos móviles")
        }
        await("Traducción del modelo local",180_000) {findNode(automation.rootInActiveWindow) {it.contentDescription=="Powered by Google Translate"}!=null}
        await("Significado real del diccionario",60_000) {textNode("1. Adjetivo")!=null}
        capture("phone-lookup-dark-android15")
        tap(text="Contexto")
        await("Oración traducida") {textNode("En esta oración")!=null}
        capture("phone-context-dark-android15")
        tap(text="Notas");await("Anotar fragmento") {textNode("Guardar subrayado")!=null};capture("phone-notes-dark-android15")
        tap(text="Contexto");tap(text="Conectar Gemini")
        await("Conexión sin abandonar el libro") {findNode(automation.rootInActiveWindow) {it.contentDescription=="Volver al libro"}!=null}
        capture("phone-gemini-from-reader-android15")
        tap(description="Volver al libro");waitForChapter("For a few precious minutes")
        touchWord("minutes",false)
        await("Ficha tras tocar palabra") {textNode("minutes")!=null}
        await("Traducción de otra palabra") {findNode(automation.rootInActiveWindow) {it.contentDescription=="Powered by Google Translate"}!=null}
        tap(text="Contexto");await("Contexto de otra palabra") {textNode("En esta oración")!=null}
        closeLookup()
        // Phrase handles remain native; this range checks the multiword payload and real model.
        assertEquals("true",javascript("""(function(){let n=document.querySelector('p').firstChild,i=n.textContent.indexOf('For a few precious minutes'),r=document.createRange();r.setStart(n,i);r.setEnd(n,n.textContent.length);let s=getSelection();s.removeAllRanges();s.addRange(r);document.dispatchEvent(new Event('selectionchange'));return true})()"""))
        await("Ficha de frase") {textNode("Traducción")!=null}
        await("Traducción de frase con modelo") {findNode(automation.rootInActiveWindow) {it.contentDescription=="Powered by Google Translate"}!=null}
        capture("phone-phrase-android15")
        closeLookup()
        tap(description="Índice y anotaciones");tap(text="A word worth keeping")
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
        tap(text="Gemini");tap(text="Conectar Gemini")
        scrollTo("Clave de Gemini")
        capture("phone-gemini-setup-android15")
    }

    @Test fun readingOptionsPersistAndReachChromium() {
        library();tap(text="Ajustes");tap(text="Diccionario y traducción");capture("phone-dictionary-settings-android15");tap(description="Volver a Ajustes");tap(text="Lectura");capture("phone-reading-text-android15")
        tap(text="Página");tap(text="Amplios");tap(text="Más");chooseTheme("Claro")
        scrollTo("Texto justificado");tap(text="Texto justificado");capture("phone-reading-page-android15")
        // Return to the tab row after inspecting the lower page controls.
        repeat(5) {findNode(automation.rootInActiveWindow) {it.isScrollable}?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);SystemClock.sleep(100)}
        tap(text="Controles");scrollTo("Consultar al tocar");tap(text="Consultar al tocar");scrollTo("Mantener pantalla encendida");tap(text="Mantener pantalla encendida");scrollTo("Mostrar ayuda de gestos");tap(text="Mostrar ayuda de gestos")
        capture("phone-reading-controls-android15")
        activity.scenario.recreate()
        await("Preferencias conservadas") {app.lectio.reader.data.SettingsStore(instrumentation.targetContext).load().let {it.margin==44&&it.readingTheme=="Claro"&&it.justified&&!it.tapToTranslate&&it.keepScreenOn&&!it.showReadingHint}}
        tap(text="Biblioteca");library();openBook()
        await("Márgenes en Chromium") {javascript("getComputedStyle(document.body).paddingLeft")=="\"44px\""}
        assertEquals("\"justify\"",javascript("getComputedStyle(document.querySelector('p')).textAlign"))
        assertEquals("\"rgb(246, 248, 252)\"",javascript("getComputedStyle(document.documentElement).backgroundColor"))
        val paragraphMargin=app.lectio.reader.data.SettingsStore(instrumentation.targetContext).load().fontSize*1.5f
        assertEquals(paragraphMargin,org.json.JSONTokener(javascript("parseFloat(getComputedStyle(document.querySelector('p')).marginBottom)")).nextValue().toString().toFloat(),.01f)
        assertNull(textNode("Desliza para pasar página. Toca una palabra para consultar; mantén pulsado para seleccionar."))
        var kept=false;activity.scenario.onActivity {kept=requireNotNull(findWebView(it.window.decorView)).keepScreenOn};assertTrue(kept)
        capture("phone-reader-custom-android15")
        tap(description="Apariencia de lectura");capture("phone-reader-appearance-android15");scrollTo("Restablecer lectura");tap(text="Restablecer lectura")
        tap(description="Cerrar apariencia")
        await("Valores iniciales restaurados") {javascript("getComputedStyle(document.body).paddingLeft")=="\"28px\""}
        tap(description="Volver a la biblioteca");library()
    }

    @Test fun pagesTurnWithoutSkippingChaptersAndKeepTextWhenReflowing() {
        val context=instrumentation.targetContext
        val file=File(context.cacheDir,"pagination-test.epub")
        java.util.zip.ZipOutputStream(file.outputStream()).use {zip ->
            fun entry(path: String,text: String) {zip.putNextEntry(java.util.zip.ZipEntry(path).apply {time=0});zip.write(text.toByteArray());zip.closeEntry()}
            entry("mimetype","application/epub+zip")
            entry("META-INF/container.xml","""<container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="content.opf"/></rootfiles></container>""")
            entry("content.opf","""<package xmlns="http://www.idpf.org/2007/opf" version="3.0"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Pagination test</dc:title><dc:creator>Lectio tests</dc:creator></metadata><manifest><item id="one" href="one.xhtml" media-type="application/xhtml+xml"/><item id="two" href="two.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="one"/><itemref idref="two"/></spine></package>""")
            val paragraphs=(1..90).joinToString("") {i -> "<p id='passage-$i'>Passage $i. "+"We read one page at a time, and every quiet sentence stays in its place. ".repeat(8)+"</p>"}
            entry("one.xhtml","<html><head><title>A long chapter</title></head><body><h1>A long chapter</h1>$paragraphs</body></html>")
            entry("two.xhtml","<html><head><title>The last chapter</title></head><body><h1>The last chapter</h1><p>Here is the final page of our test book.</p></body></html>")
        }
        activity.scenario.onActivity {host -> androidx.lifecycle.ViewModelProvider(host)[ReaderViewModel::class.java].importBook(android.net.Uri.fromFile(file))}
        await("Libro largo importado") {textNode("Pagination test")!=null}
        tap(text="Pagination test")
        activity.scenario.onActivity {host -> androidx.lifecycle.ViewModelProvider(host)[ReaderViewModel::class.java].chapter(0)}
        fun pageState(): org.json.JSONObject {
            val raw=org.json.JSONTokener(javascript("window.lectioPage && JSON.stringify(window.lectioPage())")).nextValue()
            return if(raw is String) org.json.JSONObject(raw) else org.json.JSONObject().put("page",-1).put("pages",0).put("anchor",-1)
        }
        fun chapterIndex(): Int {var index=-1;activity.scenario.onActivity {index=androidx.lifecycle.ViewModelProvider(it)[ReaderViewModel::class.java].state.value.current?.chapter ?: -1};return index}
        await("Capítulo largo paginado") {javascript("window.lectioPage && window.lectioPage().pages > 15")=="true"}
        assertEquals(0,pageState().getInt("page"))
        tap(description="Página siguiente");await("Una sola página adelante") {pageState().getInt("page")==1}
        assertEquals(0,chapterIndex())
        // Native horizontal swipe, not a synthetic DOM gesture.
        val location=IntArray(2);var width=0;var height=0
        activity.scenario.onActivity {host -> requireNotNull(findWebView(host.window.decorView)).let {it.getLocationOnScreen(location);width=it.width;height=it.height}}
        val down=SystemClock.uptimeMillis()
        for(i in 0..12) {
            val action=if(i==0) MotionEvent.ACTION_DOWN else if(i==12) MotionEvent.ACTION_UP else MotionEvent.ACTION_MOVE
            val event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,location[0]+width*(.8f-.6f*i/12),location[1]+height*.6f,0).apply {source=InputDevice.SOURCE_TOUCHSCREEN}
            try {automation.injectInputEvent(event,true)} finally {event.recycle()}
            SystemClock.sleep(16)
        }
        await("Deslizar avanza una página") {pageState().getInt("page")==2}
        assertEquals(0,chapterIndex())
        tap(description="Página anterior");await("Regreso a la página previa") {pageState().getInt("page")==1}
        val saved=pageState().getInt("anchor")
        assertTrue(saved>0)
        tap(description="Guardar marcador")
        activity.scenario.onActivity {host -> androidx.lifecycle.ViewModelProvider(host)[ReaderViewModel::class.java].preferences {it.copy(fontSize=28,margin=44)}}
        await("Texto conservado al cambiar tamaño") {pageState().getInt("anchor")==saved&&javascript("getComputedStyle(document.body).fontSize")=="\"28px\""}
        assertEquals("true",javascript("""(function(){let s=window.lectioPage();return s.page>0&&s.anchor===$saved})()"""))
        tap(description="Apariencia de lectura");tap(text="Página");chooseTheme("Papel");tap(description="Cerrar apariencia")
        await("Fondo Papel y lugar conservado") {javascript("getComputedStyle(document.documentElement).backgroundColor")=="\"rgb(246, 235, 207)\""&&pageState().getInt("anchor")==saved}
        assertEquals(saved,pageState().getInt("anchor"))
        capture("phone-paper-page-android15")
        val portraitWidth=javascript("innerWidth")
        try {
            assertTrue(automation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90))
            await("Ventana horizontal") {javascript("innerWidth")!=portraitWidth}
            await("Mismo texto tras girar") {javascript("window.lectioPage && window.lectioPage().anchor===$saved")=="true"}
        } finally {automation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_0)}
        await("Regreso a vertical conservando texto") {javascript("innerWidth")==portraitWidth&&javascript("window.lectioPage && window.lectioPage().anchor===$saved")=="true"}
        tap(description="Volver a la biblioteca");library();tap(text="Pagination test")
        await("Lugar restaurado al reabrir") {javascript("window.lectioPage && window.lectioPage().anchor===$saved")=="true"}
        // Search and fragment navigation choose the containing page, without selecting for lookup.
        javascript("window.lectioSearch('Passage 60.',0)")
        await("Resultado en página posterior") {pageState().getInt("anchor")>saved+1000}
        assertEquals("true",javascript("document.querySelector('mark[data-lectio-search]').textContent==='Passage 60.'"))
        javascript("window.lectioFragment('passage-10')")
        assertTrue(pageState().getInt("page")>0)
        javascript("window.lectioRestore(1,-1)")
        await("Última página del capítulo") {pageState().let {it.getInt("page")==it.getInt("pages")-1}}
        tap(description="Página siguiente");waitForChapter("Here is the final page")
        await("Cruce al siguiente capítulo") {chapterIndex()==1&&pageState().getInt("page")==0}
        javascript("window.lectioRestore(1,-1)")
        await("Fin del libro sin avance disponible") {
            var node=findNode(automation.rootInActiveWindow) {it.contentDescription=="Página siguiente"}
            var disabled=false
            while(node!=null) {if(!node.isEnabled) disabled=true;node=node.parent}
            disabled
        }
        capture("phone-final-page-android15")
        javascript("window.lectioRestore(0,-1)")
        await("Primera página del último capítulo") {pageState().getInt("page")==0}
        tap(description="Página anterior");waitForChapter("Passage 90.")
        await("Retroceso a la última página anterior") {chapterIndex()==0&&pageState().let {it.getInt("pages")>15&&it.getInt("page")==it.getInt("pages")-1}}
        tap(description="Volver a la biblioteca");library()
    }
}
