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
    @Test fun dictionaryLanguagesAndBookLanguageChoicesSurviveReopeningWithoutCollisions() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("lectio.db")
        LibraryDb(context).use {db ->
            db.save(Book("es","Libro","Autor","file",null,listOf(Chapter("one","Uno")),textOffset=123,language="es-CL",dictionaryLanguage="es"))
            db.cacheDictionary("pan",DictionaryEntry("pan",senses=listOf(WordSense("noun","A cooking vessel."))))
            db.cacheDictionary("pan",DictionaryEntry("pan",senses=listOf(WordSense("Sustantivo","Alimento.")),language="es",definitionLanguage="es"))
            db.save(Word("sesgos","Inclinaciones.",language="es"))
        }
        LibraryDb(context).use {db ->
            assertEquals("A cooking vessel.",db.dictionary("pan","en")!!.senses.first().definition)
            assertEquals("Alimento.",db.dictionary("pan","es")!!.senses.first().definition)
            assertEquals("es-CL",db.books().single().language)
            assertEquals("es",db.books().single().dictionaryLanguage)
            assertEquals(123,db.books().single().textOffset)
            assertEquals("es",db.words().single().language)
        }
    }
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
        val book=Book("test","Book","Author","file",null,listOf(Chapter("one","One"),Chapter("two","Two")),1,.6f,textOffset=945)
        db.save(book)
        db.save(Word("linger","perdurar",context="A scent lingers.",dictionarySource="https://en.wiktionary.org/wiki/linger#English",dictionaryLicense="https://creativecommons.org/licenses/by-sa/4.0/"))
        db.annotate(book.id,1,.6f,"a sentence","my note","highlight",945)
        db.cache("key",Lookup("linger","perdurar",contextTranslation="Un aroma perdura."))
        db.cacheDictionary("linger",DictionaryEntry("linger",senses=listOf(WordSense("verb","To stay.",spanish="Permanecer."))))
        db.addReading(10);db.addReading(20)
        db.close()
        val reopened=LibraryDb(context)
        assertEquals(.8f,reopened.books().single().progress,.001f)
        assertEquals(945,reopened.books().single().textOffset)
        assertEquals(945,reopened.annotations("test").single().textOffset)
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
        val settings=store.load().copy(readingTheme="Papel",margin=44,paragraphSpacing=1.5f,justified=true,keepScreenOn=true,tapToTranslate=false,showReadingHint=false,dictionaryEnabled=false)
        store.save(settings)
        assertEquals(settings,SettingsStore(context).load())
        context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().clear().apply()
    }
    @Test fun versionOneDatabaseMigratesWithoutLosingOldLocations() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("lectio.db")
        android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath("lectio.db"),null).use {old ->
            old.execSQL("CREATE TABLE books(id TEXT PRIMARY KEY, payload TEXT NOT NULL)")
            old.execSQL("CREATE TABLE annotations(id INTEGER PRIMARY KEY AUTOINCREMENT, book TEXT NOT NULL, chapter INTEGER NOT NULL, position REAL NOT NULL, text TEXT NOT NULL, note TEXT NOT NULL, type TEXT NOT NULL)")
            old.execSQL("INSERT INTO books VALUES(?,?)",arrayOf("legacy","""{"id":"legacy","title":"Old book","author":"Author","file":"book.epub","chapters":[{"path":"one","title":"One"}],"position":0.4}"""))
            old.execSQL("INSERT INTO annotations(book,chapter,position,text,note,type) VALUES('legacy',0,0.4,'Old place','Keep this','bookmark')")
            old.version=1
        }
        LibraryDb(context).use {db ->
            assertEquals(-1,db.books().single().textOffset)
            assertEquals(.4f,db.books().single().position,.001f)
            assertEquals(-1,db.annotations("legacy").single().textOffset)
            assertEquals("Keep this",db.annotations("legacy").single().note)
            db.annotate("legacy",0,.5f,"New place","","bookmark",500)
            assertEquals(500,db.annotations("legacy").last().textOffset)
        }
    }
}
