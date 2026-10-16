package app.lectio.reader

import app.lectio.reader.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubTest {
    @Test fun resolvesRelativeResourcesAndRejectsEscape() {
        assertEquals("OPS/Images/cover.jpg",Epub.resolve("OPS/Text/ch1.xhtml","../Images/cover.jpg"))
        assertEquals("OPS/Text/ch 2.xhtml",Epub.resolve("OPS/Text/ch1.xhtml","ch%202.xhtml#section"))
        assertEquals("OPS/Text/a+b.xhtml",Epub.resolve("OPS/Text/ch1.xhtml","a+b.xhtml"))
        assertThrows(IllegalArgumentException::class.java) {Epub.resolve("OPS/ch.xhtml","../../outside")}
        assertThrows(IllegalArgumentException::class.java) {Epub.resolve("OPS/ch.xhtml","..\\outside")}
    }
    @Test fun sampleIsAReadableEpubWithMetadataAndOrderedSpine() {
        val dir=Files.createTempDirectory("lectio-test").toFile()
        try {
            val book=DemoBook.create(dir)
            assertEquals("A Little More Slowly",book.title)
            assertEquals(3,book.chapters.size)
            assertEquals("The quiet between things",book.chapters[0].title)
            assertTrue(Epub.safeBody(Epub.resource(File(book.file),book.chapters[0].path)).contains("precious"))
        } finally {dir.deleteRecursively()}
    }
    @Test fun bookContentCannotInjectScriptsFormsOrHandlers() {
        val body=Epub.safeBody("""<body onload="bad()"><script>bad()</script><p onclick="bad()">Hello <em>reader</em></p><iframe src="https://example.com"></iframe><a href="javascript:bad()">Link</a><form><input/></form></body>""".toByteArray())
        assertTrue(body.contains("<em>reader</em>"))
        listOf("<script","onclick","onload","<iframe","javascript:","<form","<input").forEach {assertFalse(it,body.contains(it))}
    }
    @Test fun oversizedStreamsAreRejectedBeforeUnboundedAllocation() {
        assertThrows(IllegalArgumentException::class.java) {Epub.readBounded(ByteArray(20).inputStream(),10)}
        assertArrayEquals(ByteArray(10),Epub.readBounded(ByteArray(10).inputStream(),10))
    }
    @Test fun progressIsBoundedAndUsesSpinePosition() {
        val b=Book("id","Book","Author","file",null,listOf(Chapter("a","A"),Chapter("b","B")),1,.5f)
        assertEquals(.75f,b.progress,.001f)
        assertEquals(1f,b.copy(position=10f).progress,.001f)
    }
    @Test fun cacheSeparatesContextActionAndProvider() {
        val s=Selection("bank","He sat on the river bank.")
        val p=Preferences()
        assertNotEquals(AiClient.cacheKey(s,"translate",p),AiClient.cacheKey(s.copy(context="She visited the bank."),"translate",p))
        assertNotEquals(AiClient.cacheKey(s,"translate",p),AiClient.cacheKey(s,"grammar",p))
        assertNotEquals(AiClient.cacheKey(s,"translate",p),AiClient.cacheKey(s,"translate",p.copy(model="another")))
    }
    @Test fun xmlEntityExpansionIsRejected() {
        val dir=Files.createTempDirectory("lectio-xxe").toFile()
        try {
            val file=File(dir,"bad.epub")
            listOf(Charsets.UTF_8, Charsets.UTF_16).forEach { charset ->
                ZipOutputStream(file.outputStream()).use {zip->zip.putNextEntry(ZipEntry("META-INF/container.xml"));zip.write("""<!DOCTYPE container [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><container>&xxe;</container>""".toByteArray(charset));zip.closeEntry()}
                val error=assertThrows(IllegalArgumentException::class.java) {Epub.parse(file,"bad")}
                assertEquals("Declaraciones DTD no admitidas",error.message)
            }
        } finally {dir.deleteRecursively()}
    }
    @Test fun namespacedUtf16PackageRetainsMetadataAndSpine() {
        val dir=Files.createTempDirectory("lectio-xml").toFile()
        try {
            val file=File(dir,"namespaced.epub")
            ZipOutputStream(file.outputStream()).use { zip ->
                fun entry(path:String,text:String,charset:java.nio.charset.Charset=Charsets.UTF_8) {
                    zip.putNextEntry(ZipEntry(path));zip.write(text.toByteArray(charset));zip.closeEntry()
                }
                entry("META-INF/container.xml","""<c:container xmlns:c="urn:oasis:names:tc:opendocument:xmlns:container"><c:rootfiles><c:rootfile full-path="OPS/book.opf"/></c:rootfiles></c:container>""",Charsets.UTF_16)
                entry("OPS/book.opf","""<p:package xmlns:p="http://www.idpf.org/2007/opf" xmlns:dc="http://purl.org/dc/elements/1.1/"><p:metadata><dc:title>El jardín &amp; el río</dc:title><dc:creator>Lucía</dc:creator></p:metadata><p:manifest><p:item id="one" href="one.xhtml" media-type="application/xhtml+xml"/></p:manifest><p:spine><p:itemref idref="one"/></p:spine></p:package>""",Charsets.UTF_16)
                entry("OPS/one.xhtml","<html><head><title>Primero</title></head><body><p>Una lectura.</p></body></html>")
            }
            val book=Epub.parse(file,"namespaced")
            assertEquals("El jardín & el río",book.title)
            assertEquals("Lucía",book.author)
            assertEquals(listOf(Chapter("OPS/one.xhtml","Primero")),book.chapters)
        } finally {dir.deleteRecursively()}
    }
}
