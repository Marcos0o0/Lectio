package app.lectio.reader

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import app.lectio.reader.ui.readerResourceResponse
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ReaderResourceTest {
    private val context: Context get()=ApplicationProvider.getApplicationContext()

    @Test fun internalDocumentsLoadAndPathlessRequestsNeverCrash() {
        val missing=File(context.cacheDir,"missing.epub")
        listOf("about:blank","data:text/html,<html><body>A chapter</body></html>").forEach { url ->
            // null delegates internal document loading to WebView; an empty response blanks the book.
            assertNull(readerResourceResponse(context,missing,Uri.parse(url)))
        }
        listOf("https://lectio.local","https://lectio.local/",
            "https://lectio.local/book/","https://example.com/remote.png","file:///etc/passwd").forEach { url ->
            val response=requireNotNull(readerResourceResponse(context,missing,Uri.parse(url)))
            response.data.use { assertEquals(url,0,it.readBytes().size) }
        }
    }

    @Test fun onlyPassiveResourcesInsideTheBookAreServed() {
        val file=File.createTempFile("resources-", ".epub",context.cacheDir)
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                listOf("OPS/picture.png","OPS/script.js","OPS/page.xhtml","OPS/image.svg").forEach { path ->
                    zip.putNextEntry(ZipEntry(path));zip.write(byteArrayOf(1,2,3));zip.closeEntry()
                }
            }
            val image=requireNotNull(readerResourceResponse(context,file,Uri.parse("https://lectio.local/book/OPS/picture.png")))
            assertEquals("image/png",image.mimeType)
            image.data.use {assertArrayEquals(byteArrayOf(1,2,3),it.readBytes())}
            listOf("OPS/script.js","OPS/page.xhtml","OPS/image.svg","../../outside","OPS/missing.png").forEach { path ->
                requireNotNull(readerResourceResponse(context,file,Uri.parse("https://lectio.local/book/$path"))).data.use {assertEquals(0,it.readBytes().size)}
            }
        } finally {file.delete()}
    }

    @Test fun bundledReaderFontIsAvailableWithoutNetwork() {
        val font=requireNotNull(readerResourceResponse(context,File("unused"),Uri.parse("https://lectio.local/app-font/literata.ttf")))
        assertEquals("font/ttf",font.mimeType)
        font.data.use {assertTrue(it.readBytes().size>100_000)}
    }
}
