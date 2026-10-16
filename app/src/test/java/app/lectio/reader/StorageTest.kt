package app.lectio.reader

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.lectio.reader.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class StorageTest {
    @Test fun importingSameEpubTwicePreservesItsProgress() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("lectio.db")
        val fixture=DemoBook.create(context.cacheDir)
        val repo=ReaderRepository(context)
        val first=repo.import(android.net.Uri.fromFile(java.io.File(fixture.file)))
        repo.db.save(first.copy(chapter=1,position=.4f))
        val second=repo.import(android.net.Uri.fromFile(java.io.File(fixture.file)))
        assertEquals(first.id,second.id)
        assertEquals(1,repo.db.books().size)
        assertEquals(.4f,second.position,.001f)
        assertTrue(java.io.File(first.file).exists())
        repo.db.close()
    }
    @Test fun progressVocabularyAnnotationsCacheAndTimeSurviveDatabaseReopen() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("lectio.db")
        val db=LibraryDb(context)
        val book=Book("test","Book","Author","file",null,listOf(Chapter("one","One"),Chapter("two","Two")),1,.6f)
        db.save(book)
        db.save(Word("linger","perdurar",context="A scent lingers.",dictionarySource="https://en.wiktionary.org/wiki/linger#English",dictionaryLicense="https://creativecommons.org/licenses/by-sa/4.0/"))
        db.annotate(book.id,1,.6f,"a sentence","my note","highlight")
        db.cache("key",Lookup("linger","perdurar",contextTranslation="Un aroma perdura."))
        db.cacheDictionary("linger",DictionaryEntry("linger",senses=listOf(WordSense("verb","To stay.",spanish="Permanecer."))))
        db.addReading(10);db.addReading(20)
        db.close()
        val reopened=LibraryDb(context)
        assertEquals(.8f,reopened.books().single().progress,.001f)
        assertEquals("perdurar",reopened.words().single().translation)
        assertEquals("https://en.wiktionary.org/wiki/linger#English",reopened.words().single().dictionarySource)
        assertEquals("my note",reopened.annotations("test").single().note)
        assertTrue(reopened.cached("key")!!.cached)
        assertEquals("Un aroma perdura.",reopened.cached("key")!!.contextTranslation)
        assertEquals("Permanecer.",reopened.dictionary("linger")!!.senses.single().spanish)
        assertEquals(30,reopened.reading().values.sum())
        reopened.deleteBook("test")
        assertTrue(reopened.annotations("test").isEmpty())
        assertEquals(1,reopened.words().size)
        reopened.close()
    }
    @Test fun readingOptionsPersistAndOldInstallsGetCompatibleDefaults() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().clear().putInt("fontSize",24).apply()
        val store=SettingsStore(context)
        assertEquals(24,store.load().fontSize)
        assertEquals("Aplicación",store.load().readingTheme)
        assertTrue(store.load().tapToTranslate)
        val settings=store.load().copy(readingTheme="Claro",margin=44,paragraphSpacing=1.5f,justified=true,keepScreenOn=true,tapToTranslate=false,showReadingHint=false,dictionaryEnabled=false)
        store.save(settings)
        assertEquals(settings,SettingsStore(context).load())
        context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().clear().apply()
    }
}
