package app.lectio.reader.data

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.jsoup.nodes.Element
import org.jsoup.nodes.DocumentType
import org.jsoup.parser.Parser
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.util.zip.ZipFile

/** ZIPs stay compressed on disk. No archive-controlled path is ever written to the filesystem. */
object Epub {
    const val MAX_BOOK_BYTES = 100L * 1024 * 1024
    const val MAX_RESOURCE_BYTES = 16 * 1024 * 1024

    fun resolve(base: String, href: String): String {
        val decoded = URLDecoder.decode(href.substringBefore('#').substringBefore('?').replace("+", "%2B"), "UTF-8")
        require(!decoded.contains('\\') && !decoded.contains('\u0000')) { "Ruta EPUB no válida" }
        val parts = mutableListOf<String>()
        val path = if (decoded.startsWith('/')) decoded.drop(1) else base.substringBeforeLast('/', "").let { if (it.isEmpty()) decoded else "$it/$decoded" }
        path.split('/').forEach { when (it) { "", "." -> Unit; ".." -> { require(parts.isNotEmpty()) { "Ruta fuera del EPUB" }; parts.removeAt(parts.lastIndex) }; else -> parts.add(it) } }
        return parts.joinToString("/")
    }

    fun resource(file: File, path: String): ByteArray = ZipFile(file).use { zip ->
        val entry = zip.getEntry(path) ?: error("No se encontró el recurso del libro")
        require(entry.size <= MAX_RESOURCE_BYTES) { "Recurso demasiado grande" }
        zip.getInputStream(entry).use { readBounded(it, MAX_RESOURCE_BYTES) }
    }

    fun readBounded(input: InputStream, max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= max) { "El recurso supera el límite permitido" }
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    // Use the bundled parser on Android and JVM alike. Jsoup does not resolve DTDs
    // or external entities; reject declarations explicitly rather than accept them.
    private fun xml(bytes: ByteArray) = bytes.inputStream().use {
        Jsoup.parse(it, null, "", Parser.xmlParser()).also { doc ->
            require(doc.childNodes().none { node -> node is DocumentType }) { "Declaraciones DTD no admitidas" }
        }
    }

    private fun Element.elements(name: String) = getAllElements().filter { it.tagName().substringAfterLast(':') == name }

    fun parse(file: File, id: String): Book {
        val container = xml(resource(file, "META-INF/container.xml"))
        val root = container.elements("rootfile").firstOrNull() ?: error("EPUB sin paquete de contenido")
        val opfPath = resolve("", root.attr("full-path"))
        val opf = xml(resource(file, opfPath))
        fun metadata(name: String, fallback: String) = opf.elements(name).firstOrNull()?.text()?.trim()?.takeIf { it.isNotEmpty() } ?: fallback
        val manifest = mutableMapOf<String, Pair<String, Element>>()
        for (item in opf.elements("item")) {
            manifest[item.attr("id")] = resolve(opfPath, item.attr("href")) to item
        }
        val labels = mutableMapOf<String, String>()
        manifest.values.firstOrNull { it.second.attr("properties").split(' ').contains("nav") }?.let { nav ->
            Jsoup.parse(String(resource(file, nav.first), Charsets.UTF_8)).select("nav a[href]").forEach { link ->
                runCatching { labels[resolve(nav.first, link.attr("href"))] = link.text() }
            }
        }
        manifest.values.firstOrNull { it.second.attr("media-type") == "application/x-dtbncx+xml" }?.let { ncx ->
            val doc = Jsoup.parse(String(resource(file, ncx.first), Charsets.UTF_8), "", org.jsoup.parser.Parser.xmlParser())
            doc.select("navPoint").forEach { point -> runCatching {
                labels.putIfAbsent(resolve(ncx.first, point.selectFirst("content")!!.attr("src")), point.selectFirst("text")!!.text())
            } }
        }
        val chapters = mutableListOf<Chapter>()
        for (ref in opf.elements("itemref")) {
            val item = manifest[ref.attr("idref")] ?: continue
            if (ref.attr("linear") == "no" || !item.second.attr("media-type").contains("html")) continue
            val doc = Jsoup.parse(String(resource(file, item.first), Charsets.UTF_8))
            val title = labels[item.first] ?: doc.selectFirst("h1, h2, title")?.text()?.takeIf { it.isNotBlank() } ?: "Capítulo ${chapters.size + 1}"
            chapters.add(Chapter(item.first, title))
        }
        require(chapters.isNotEmpty()) { "No hay capítulos legibles. Verifica que el EPUB no tenga DRM." }
        val coverId = opf.elements("meta").firstOrNull { it.attr("name") == "cover" }?.attr("content")
        val cover = manifest.values.firstOrNull { it.second.attr("properties").split(' ').contains("cover-image") }?.first ?: manifest[coverId]?.first
        return Book(id, metadata("title", "Sin título"), metadata("creator", "Autor desconocido"), file.absolutePath, cover, chapters,language=metadata("language",""))
    }

    fun language(file: File): String {
        val root=xml(resource(file,"META-INF/container.xml")).elements("rootfile").firstOrNull() ?: return ""
        return xml(resource(file,resolve("",root.attr("full-path")))).elements("language").firstOrNull()?.text()?.trim().orEmpty()
    }

    /** Active content from books is removed before our own selection bridge is added. */
    fun safeBody(bytes: ByteArray): String {
        val doc = Jsoup.parse(String(bytes, Charsets.UTF_8))
        doc.select("script, style, iframe, object, embed, form, input, button, video, audio").remove()
        val safe = Safelist.relaxed().addTags("section", "article", "aside", "figure", "figcaption", "sup", "sub")
            .addAttributes(":all", "id", "lang", "dir").addAttributes("img", "src", "alt", "width", "height")
            .removeProtocols("img", "src", "http", "https").preserveRelativeLinks(true)
        return Jsoup.clean(doc.body().html(), "", safe, org.jsoup.nodes.Document.OutputSettings().prettyPrint(false))
    }
}
