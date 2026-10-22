package app.lectio.reader

import app.lectio.reader.data.*
import org.junit.Assert.*
import org.junit.Test

class ReadingLanguageTest {
    private val book=Book("id","Title","Author","file",null,listOf(Chapter("one","One")))
    @Test fun metadataAndExplicitChoiceTakePrecedenceOverContext() {
        val selection=Selection("pan","El pan de la casa.")
        assertEquals("es",ReadingLanguage.resolve(book.copy(language="es-CL"),selection))
        assertEquals("en",ReadingLanguage.resolve(book.copy(language="es",dictionaryLanguage="en"),selection))
        assertEquals("en",ReadingLanguage.resolve(book.copy(language="en_US"),selection))
    }
    @Test fun oldBooksUseSentenceHintsAndUnicodeWithoutInventingDefinitions() {
        assertEquals("es",ReadingLanguage.resolve(book,Selection("sesgos","Los sesgos de la autora.")))
        assertEquals("es",ReadingLanguage.resolve(book,Selection("árbol")))
        assertEquals("en",ReadingLanguage.resolve(book,Selection("precious","For a few precious minutes, the world was quiet.")))
        assertNull(ReadingLanguage.code("fr"))
    }
}
