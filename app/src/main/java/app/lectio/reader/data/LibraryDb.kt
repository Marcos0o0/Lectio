package app.lectio.reader.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

class LibraryDb(context: Context) : SQLiteOpenHelper(context, "lectio.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE books(id TEXT PRIMARY KEY, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE words(word TEXT PRIMARY KEY, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE annotations(id INTEGER PRIMARY KEY AUTOINCREMENT, book TEXT NOT NULL, chapter INTEGER NOT NULL, position REAL NOT NULL, text TEXT NOT NULL, note TEXT NOT NULL, type TEXT NOT NULL, textOffset INTEGER NOT NULL DEFAULT -1)")
        db.execSQL("CREATE TABLE cache(key TEXT PRIMARY KEY, payload TEXT NOT NULL)")
        db.execSQL("CREATE TABLE sessions(day TEXT PRIMARY KEY, seconds INTEGER NOT NULL DEFAULT 0)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if(oldVersion<2) db.execSQL("ALTER TABLE annotations ADD COLUMN textOffset INTEGER NOT NULL DEFAULT -1")
    }
    private fun put(table: String, idColumn: String, id: String, payload: JSONObject) {
        writableDatabase.insertWithOnConflict(table, null, ContentValues().apply { put(idColumn, id); put("payload", payload.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
    }
    fun books(): List<Book> = readableDatabase.rawQuery("SELECT payload FROM books", null).use { c -> buildList { while(c.moveToNext()) add(book(JSONObject(c.getString(0)))) } }.sortedByDescending { maxOf(it.lastRead, it.added) }
    fun save(book: Book) = put("books", "id", book.id, JSONObject().apply {
        put("id", book.id); put("title", book.title); put("author", book.author); put("file", book.file); put("cover", book.cover)
        put("chapters", JSONArray().apply { book.chapters.forEach { put(JSONObject().put("path", it.path).put("title", it.title)) } })
        put("chapter", book.chapter); put("position", book.position); put("lastRead", book.lastRead); put("added", book.added); put("demo", book.demo);put("textOffset",book.textOffset)
    })
    private fun book(j: JSONObject) = Book(j.getString("id"), j.getString("title"), j.getString("author"), j.getString("file"), j.optString("cover").takeIf { it.isNotEmpty() && it != "null" },
        j.getJSONArray("chapters").let { a -> (0 until a.length()).map { a.getJSONObject(it).let { Chapter(it.getString("path"), it.getString("title")) } } },
        j.optInt("chapter"), j.optDouble("position", 0.0).toFloat(), j.optLong("lastRead"), j.optLong("added"), j.optBoolean("demo"),j.optInt("textOffset",-1))
    fun deleteBook(id: String) { writableDatabase.beginTransaction(); try { writableDatabase.delete("books", "id=?", arrayOf(id)); writableDatabase.delete("annotations", "book=?", arrayOf(id)); writableDatabase.setTransactionSuccessful() } finally { writableDatabase.endTransaction() } }
    fun words(): List<Word> = readableDatabase.rawQuery("SELECT payload FROM words ORDER BY word", null).use { c -> buildList { while(c.moveToNext()) { val j=JSONObject(c.getString(0)); add(Word(j.getString("word"),j.getString("translation"),j.optString("definition"),j.optString("example"),j.optString("synonyms"),j.optString("level"),j.optString("book"),j.optString("context"),j.optBoolean("learned"),j.optLong("due"),j.optInt("interval"),j.optString("source", "Diccionario local"),j.optString("dictionarySource"),j.optString("dictionaryLicense"))) } } }
    fun save(word: Word) = put("words", "word", word.text.lowercase(), JSONObject().apply {
        put("word",word.text.lowercase()); put("translation",word.translation); put("definition",word.definition); put("example",word.example); put("synonyms",word.synonyms); put("level",word.level); put("book",word.book); put("context",word.context); put("learned",word.learned); put("due",word.due); put("interval",word.interval); put("source",word.source);put("dictionarySource",word.dictionarySource);put("dictionaryLicense",word.dictionaryLicense)
    })
    fun deleteWord(word: String) { writableDatabase.delete("words", "word=?", arrayOf(word.lowercase())) }
    fun annotations(book: String) = readableDatabase.rawQuery("SELECT * FROM annotations WHERE book=? ORDER BY chapter, position", arrayOf(book)).use { c -> buildList { while(c.moveToNext()) add(Annotation(c.getLong(0),c.getString(1),c.getInt(2),c.getFloat(3),c.getString(4),c.getString(5),c.getString(6),c.getInt(7))) } }
    fun annotate(book: String, chapter: Int, position: Float, text: String, note: String, type: String, textOffset: Int = -1) {
        writableDatabase.insert("annotations",null,ContentValues().apply { put("book",book); put("chapter",chapter); put("position",position); put("text",text); put("note",note); put("type",type);put("textOffset",textOffset) })
    }
    fun deleteAnnotation(id: Long) { writableDatabase.delete("annotations","id=?",arrayOf(id.toString())) }
    fun cached(key: String): Lookup? = readableDatabase.rawQuery("SELECT payload FROM cache WHERE key=?", arrayOf(key)).use { c -> if (!c.moveToFirst()) null else JSONObject(c.getString(0)).let { Lookup(it.getString("original"),it.getString("translation"),it.optString("definition"),it.optString("example"),it.optString("synonyms"),it.optString("level"),it.optString("source"),true,it.optString("contextTranslation")) } }
    fun cache(key: String, value: Lookup) = put("cache","key",key,JSONObject().apply { put("original",value.original); put("translation",value.translation); put("definition",value.definition); put("example",value.example); put("synonyms",value.synonyms); put("level",value.level); put("source",value.source); put("contextTranslation",value.contextTranslation) })
    fun dictionary(word: String): DictionaryEntry? = readableDatabase.rawQuery("SELECT payload FROM cache WHERE key=?",arrayOf("wiktionary-v1:$word")).use { c -> if(c.moveToFirst()) DictionaryClient.decode(JSONObject(c.getString(0))) else null }
    fun cacheDictionary(word: String, entry: DictionaryEntry) = put("cache","key","wiktionary-v1:$word",DictionaryClient.encode(entry))
    fun addReading(seconds: Int) { val day=java.time.LocalDate.now().toString(); writableDatabase.beginTransaction(); try { writableDatabase.execSQL("INSERT OR IGNORE INTO sessions(day,seconds) VALUES(?,0)", arrayOf(day)); writableDatabase.execSQL("UPDATE sessions SET seconds=seconds+? WHERE day=?", arrayOf(seconds,day)); writableDatabase.setTransactionSuccessful() } finally { writableDatabase.endTransaction() } }
    fun reading(): Map<String, Int> = readableDatabase.rawQuery("SELECT day,seconds FROM sessions ORDER BY day",null).use { c -> buildMap { while(c.moveToNext()) put(c.getString(0),c.getInt(1)) } }
}
