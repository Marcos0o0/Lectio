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
    private val root: AccessibilityNodeInfo? get() {
        // Compose may reuse semantic IDs after a theme change; do not query a cached screen.
        if(android.os.Build.VERSION.SDK_INT>=33) automation.clearCache()
        return automation.rootInActiveWindow
    }

    private fun await(description: String, timeout: Long = 30_000, condition: () -> Boolean) {
        val deadline=SystemClock.elapsedRealtime()+timeout
        do {
            if(condition()) return
            SystemClock.sleep(100)
        } while(SystemClock.elapsedRealtime()<deadline)
        capture("phone-native-failure-android15")
        fail("No se cumplió: $description")
    }

    private fun findNode(root: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        root ?: return null
        if(predicate(root)) return root
        for(i in 0 until root.childCount) findNode(root.getChild(i),predicate)?.let {return it}
        return null
    }

    private fun textNode(text: String)=findNode(root) {it.text?.toString()==text}
    private fun tap(text: String? = null, description: String? = null) {
        val label=text ?: description.orEmpty()
        await("Control activado: $label") {
            var target=findNode(root) {
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
        // Navigation can retain a list's scroll position; the target may be above it.
        for(i in 0 until 20) {
            if(textNode(text)?.isVisibleToUser==true) return
            val scroll=findNode(root) {it.isScrollable} ?: break
            if(scroll.actionList.none {it.id==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD}) break
            scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            SystemClock.sleep(100)
        }
        repeat(20) {
            if(textNode(text)?.isVisibleToUser==true) return
            val scroll=findNode(root) {it.isScrollable}
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
        try {
            await("Biblioteca preparada") {textNode("Lectio")!=null}
            automation.waitForIdle(150,5000)
            scrollTo("A Little More Slowly")
            tap(text="A Little More Slowly")
        } catch(e: AssertionError) {capture("phone-open-book-test-failure");throw e}

        await("Lector visible") {findNode(root) {it.contentDescription=="Volver a la biblioteca"}!=null}
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

    @Test fun libraryOrganizationPersistsWithoutChangingReadingProgress() {
        val context=instrumentation.targetContext
        val sample=app.lectio.reader.data.DemoBook.create(context.cacheDir)
        val fixtures=listOf(
            sample.copy(id="library-test-a",title="Muestra: Árboles al viento",author="Lectio · muestra de biblioteca",lastRead=10,added=1,position=.3f),
            sample.copy(id="library-test-b",title="Muestra: Mirar despacio",author="Lectio · muestra de biblioteca",lastRead=0,added=3),
            sample.copy(id="library-test-c",title="Muestra: Un camino largo entre las páginas de un libro",author="Lectio · muestra de biblioteca",lastRead=0,added=2)
        )
        app.lectio.reader.data.LibraryDb(context).use {db -> fixtures.forEach {db.save(it)}}
        activity.scenario.onActivity {host -> androidx.lifecycle.ViewModelProvider(host)[ReaderViewModel::class.java].preferences {
            it.copy(librarySort="RECENT",favoriteBooks=it.favoriteBooks-fixtures.map {b -> b.id}.toSet())
        }}
        await("Orden inicial guardado") {app.lectio.reader.data.SettingsStore(context).load().librarySort=="RECENT"}
        activity.scenario.recreate()
        library()
        tap(description="Buscar libros")
        fun search(value: String) {
            await("Campo de búsqueda") {
                val field=findNode(root) {it.isEditable}
                field?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,android.os.Bundle().apply {putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value)})==true
            }
            instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER)
            automation.waitForIdle(150,3000)
        }
        search("MUESTRA: arboles")
        scrollTo(fixtures[0].title)
        await("Título sin tilde encontrado") {textNode(fixtures[0].title)!=null&&textNode(fixtures[1].title)==null}
        capture("phone-library-search-android15")
        search("Muestra:")
        scrollTo("Orden: Recientes");tap(text="Orden: Recientes")
        await("Menú de orden desplegado") {textNode("Últimos añadidos")!=null}
        capture("phone-library-sort-menu-android15")
        tap(text="Título")
        await("Orden persistido") {app.lectio.reader.data.SettingsStore(context).load().librarySort=="TITLE"}
        fixtures.take(2).forEach {book ->
            scrollTo(book.title)
            tap(description="Añadir a favoritos: ${book.title}")
        }
        scrollTo("Favoritos");tap(text="Favoritos")
        await("Dos favoritos guardados") {app.lectio.reader.data.SettingsStore(context).load().favoriteBooks.containsAll(fixtures.take(2).map {it.id})}
        activity.scenario.recreate()
        library()
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER)
        await("Búsqueda y filtro restaurados") {
            var node=textNode("Favoritos");var selected=false
            while(node!=null) {if(node.isSelected||node.isChecked) selected=true;node=node.parent}
            selected&&textNode("Orden: Título")!=null
        }
        fixtures.take(2).forEach {book ->
            scrollTo(book.title)
            tap(description="Quitar de favoritos: ${book.title}")
        }
        // Close search to show the favorites-specific recovery, rather than search's empty state.
        tap(description="Cerrar búsqueda")
        scrollTo("Aún no tienes favoritos")
        capture("phone-library-favorites-empty-android15")
        scrollTo("Todos");tap(text="Todos")
        fixtures.take(2).forEach {book ->
            scrollTo(book.title)
            tap(description="Añadir a favoritos: ${book.title}")
        }
        scrollTo("Favoritos");tap(text="Favoritos")
        scrollTo("Orden: Título")
        capture("phone-library-favorites-android15")
        app.lectio.reader.data.LibraryDb(context).use {db ->
            val stored=db.books().first {it.id==fixtures[0].id}
            assertEquals(.3f,stored.position,.001f)
            assertEquals(10L,stored.lastRead)
        }
        assertEquals("TITLE",app.lectio.reader.data.SettingsStore(context).load().librarySort)
        assertEquals(2,app.lectio.reader.data.SettingsStore(context).load().favoriteBooks.intersect(fixtures.map {it.id}.toSet()).size)
    }

    /** Find geometry only; the selection itself must come from real Android touch events. */
    private fun touchWord(word: String, longPress: Boolean) {
        await("Lector con foco táctil") {var focused=false;activity.scenario.onActivity {focused=it.hasWindowFocus()};focused}
        automation.waitForIdle(150,5000)
        val raw=javascript("""(function(){let n=document.querySelector('p').firstChild,i=n.textContent.indexOf(${org.json.JSONObject.quote(word)});let r=document.createRange();r.setStart(n,i);r.setEnd(n,i+${word.length});let b=r.getClientRects()[0]||r.getBoundingClientRect();return JSON.stringify({x:b.x+b.width/2,y:b.y+b.height/2,scale:devicePixelRatio})})()""")
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
        await("Ficha cerrada") {findNode(root) {it.contentDescription=="Cerrar consulta"}==null}
        await("Lector con foco") {var focused=false;activity.scenario.onActivity {focused=it.hasWindowFocus()};focused}
        automation.waitForIdle(150,5000)
        await("Selección retirada") {javascript("getSelection().toString()")=="\"\""}
    }
    private fun enterImmersive() {
        tap(description="Leer a pantalla completa")
        await("Pantalla completa disponible") {
            // Android's first-use education window consumes touches until acknowledged.
            val education=textNode("Got it") ?: textNode("Entendido")
            if(education!=null) {education.performAction(AccessibilityNodeInfo.ACTION_CLICK);false}
            else findNode(root) {it.contentDescription=="Salir de pantalla completa"}!=null&&findNode(root) {it.contentDescription=="Página siguiente"}==null
        }
        automation.waitForIdle(300,5000)
    }

    @Test fun spanishWordsResolveRealDefinitionsAndKeepTheChosenLanguage() {
        val context=instrumentation.targetContext
        val file=File(context.cacheDir,"spanish-test.epub")
        java.util.zip.ZipOutputStream(file.outputStream()).use {zip ->
            fun entry(path: String,text: String) {zip.putNextEntry(java.util.zip.ZipEntry(path));zip.write(text.toByteArray());zip.closeEntry()}
            entry("META-INF/container.xml","""<container><rootfiles><rootfile full-path="content.opf"/></rootfiles></container>""")
            entry("content.opf","""<package><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Lectura en español</dc:title><dc:language>es-CL</dc:language></metadata><manifest><item id="one" href="one.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="one"/></spine></package>""")
            entry("one.xhtml","<html><head><title>Palabras</title></head><body><h1>Palabras</h1><p>Patriarcado feminista ensombrecieron despolitizacion metalinguisticas sesgos. Los sesgos de la autora aparecen en la lectura.</p></body></html>")
        }
        fun vm(action: (ReaderViewModel)->Unit) {activity.scenario.onActivity {action(androidx.lifecycle.ViewModelProvider(it)[ReaderViewModel::class.java])}}
        fun current(): ReaderState {lateinit var state: ReaderState;vm {state=it.state.value};return state}
        vm {it.importBook(android.net.Uri.fromFile(file))}
        await("Libro español disponible") {current().books.any {it.title=="Lectura en español"}}
        scrollTo("Lectura en español")
        tap(text="Lectura en español")
        waitForChapter("Patriarcado feminista")
        enterImmersive()
        await("Pantalla completa") {findNode(root) {it.contentDescription=="Salir de pantalla completa"}!=null&&findNode(root) {it.contentDescription=="Página siguiente"}==null}
        automation.waitForIdle(300,5000)
        for(word in listOf("Patriarcado","feminista","ensombrecieron","despolitizacion","metalinguisticas","sesgos")) {
            touchWord(word,false)
            await("Palabra seleccionada por toque: $word") {current().selection?.text==word}
            await("Significado español real: $word",60_000) {current().let {
                if(it.definitionError!=null) throw AssertionError("$word: ${it.definitionError}; consulta directa",runCatching {app.lectio.reader.data.DictionaryClient.fetch(word.lowercase(),"es")}.exceptionOrNull())
                it.dictionary!=null&&!it.definitionLoading
            }}
            val state=current()
            assertEquals("es",state.lookupLanguage)
            assertNull(state.lookup)
            assertFalse(state.translationNeedsDownload)
            assertEquals("es",state.dictionary!!.definitionLanguage)
            assertTrue(state.dictionary!!.senses.first().definition.isNotBlank())
            if(word=="feminista") {
                assertTrue(state.dictionary!!.senses.any {it.definition.contains("feminismo")})
                assertTrue(state.dictionary!!.relatedWords.contains("feminismo"))
                assertFalse(state.dictionary!!.senses.any {it.definition.contains("hispana")})
                capture("phone-spanish-feminista-android15")
            }
            if(word=="ensombrecieron") {assertEquals("ensombrecer",state.dictionary!!.lemma);assertTrue(state.dictionary!!.senses.first().partOfSpeech.startsWith("Verbo"));capture("phone-spanish-verb-android15")}
            if(word=="despolitizacion") assertEquals("despolitización",state.dictionary!!.word)
            if(word=="metalinguisticas") assertEquals("metalingüístico",state.dictionary!!.lemma)
            if(word=="sesgos") {
                tap(text=if(state.words.any {it.text=="sesgos"}) "Actualizar vocabulario" else "Guardar en vocabulario")
                await("Guardar definición sin traducir ni conectar Gemini") {current().words.any {it.text=="sesgos"&&it.language=="es"&&it.translation==state.dictionary!!.senses.first().definition}}
                tap(text="Inglés");await("Idioma inglés elegido") {current().lookupLanguage=="en"}
                tap(text="Español");await("Definición española recuperada de caché") {current().dictionary?.cached==true&&current().lookupLanguage=="es"}
            }
            closeLookup()
        }
        tap(description="Salir de pantalla completa")
        tap(description="Volver a la biblioteca");library();tap(text="Lectura en español")
        waitForChapter("Patriarcado feminista")
        touchWord("sesgos",true)
        await("Idioma por libro conservado") {current().dictionary?.cached==true&&current().current?.dictionaryLanguage=="es"}
        capture("phone-spanish-cached-android15")
        closeLookup()
        tap(description="Volver a la biblioteca");library()
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
        await("Traducción o descarga disponible") {textNode("Descargar por Wi-Fi")!=null||findNode(root) {it.contentDescription=="Powered by Google Translate"}!=null}
        if(textNode("Descargar por Wi-Fi")!=null) {
            capture("phone-download-android15")
            tap(text="Descargar usando datos móviles")
        }
        await("Traducción del modelo local",180_000) {findNode(root) {it.contentDescription=="Powered by Google Translate"}!=null}
        await("Significado real del diccionario",60_000) {textNode("1. Adjetivo")!=null}
        capture("phone-lookup-dark-android15")
        tap(text="Contexto")
        await("Oración traducida") {textNode("En esta oración")!=null}
        capture("phone-context-dark-android15")
        tap(text="Notas");await("Anotar fragmento") {textNode("Guardar subrayado")!=null};capture("phone-notes-dark-android15")
        tap(text="Contexto");tap(text="Conectar Gemini")
        await("Conexión sin abandonar el libro") {findNode(root) {it.contentDescription=="Volver al libro"}!=null}
        capture("phone-gemini-from-reader-android15")
        tap(description="Volver al libro");waitForChapter("For a few precious minutes")
        touchWord("minutes",false)
        await("Ficha tras tocar palabra") {textNode("minutes")!=null}
        await("Traducción de otra palabra") {findNode(root) {it.contentDescription=="Powered by Google Translate"}!=null}
        tap(text="Contexto");await("Contexto de otra palabra") {textNode("En esta oración")!=null}
        closeLookup()
        // Phrase handles remain native; this range checks the multiword payload and real model.
        assertEquals("true",javascript("""(function(){let n=document.querySelector('p').firstChild,i=n.textContent.indexOf('For a few precious minutes'),r=document.createRange();r.setStart(n,i);r.setEnd(n,n.textContent.length);let s=getSelection();s.removeAllRanges();s.addRange(r);document.dispatchEvent(new Event('selectionchange'));return true})()"""))
        await("Ficha de frase") {textNode("Traducción")!=null}
        await("Traducción de frase con modelo") {findNode(root) {it.contentDescription=="Powered by Google Translate"}!=null}
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
        repeat(5) {findNode(root) {it.isScrollable}?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);SystemClock.sleep(100)}
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
        await("Libro largo importado") {
            var imported=false
            activity.scenario.onActivity {host -> imported=androidx.lifecycle.ViewModelProvider(host)[ReaderViewModel::class.java].state.value.books.any {it.title=="Pagination test"}}
            imported
        }
        scrollTo("Pagination test")
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
        val initialAnchor=pageState().getInt("anchor")
        enterImmersive()
        await("Entrada inmersiva conserva texto y oculta barras",15_000) {
            var hidden=false
            activity.scenario.onActivity {host -> hidden=androidx.core.view.ViewCompat.getRootWindowInsets(host.window.decorView)?.isVisible(androidx.core.view.WindowInsetsCompat.Type.systemBars())==false}
            hidden&&pageState().getInt("anchor")==initialAnchor&&findNode(root) {it.contentDescription=="Salir de pantalla completa"}!=null&&findNode(root) {it.contentDescription=="Página siguiente"}==null
        }
        assertNull(findNode(root) {it.contentDescription=="Página siguiente"})
        capture("phone-immersive-dark-android15")
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
        await("Deslizar avanza una página inmersiva") {pageState().getInt("anchor")>initialAnchor}
        assertEquals(0,chapterIndex())
        assertTrue(automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
        await("Atrás sale de inmersivo sin cerrar libro") {findNode(root) {it.contentDescription=="Página siguiente"}!=null}
        // Returning to the smaller viewport reflows around the text, not the old page number.
        javascript("window.lectioRestore(0,-1)")
        await("Inicio del capítulo") {pageState().getInt("page")==0}
        tap(description="Página siguiente");await("Página uno") {pageState().getInt("page")==1}
        tap(description="Página siguiente");await("Página dos") {pageState().getInt("page")==2}
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
        enterImmersive()
        await("Papel inmersivo conserva texto") {pageState().getInt("anchor")==saved}
        capture("phone-immersive-paper-android15")
        val portraitWidth=javascript("innerWidth")
        try {
            assertTrue(automation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90))
            await("Ventana horizontal") {javascript("innerWidth")!=portraitWidth}
            await("Mismo texto tras girar") {javascript("window.lectioPage && window.lectioPage().anchor===$saved")=="true"}
        } finally {automation.setRotation(android.app.UiAutomation.ROTATION_FREEZE_0)}
        await("Regreso a vertical conservando texto") {javascript("innerWidth")==portraitWidth&&javascript("window.lectioPage && window.lectioPage().anchor===$saved")=="true"}
        tap(description="Salir de pantalla completa")
        await("Lugar al salir de pantalla completa") {pageState().getInt("anchor")==saved}
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
            var node=findNode(root) {it.contentDescription=="Página siguiente"}
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
