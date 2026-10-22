package app.lectio.reader.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest

class ReaderRepository(private val context: Context) {
    val db = LibraryDb(context)
    val settings = SettingsStore(context)
    val translation by lazy { LocalTranslation() }
    private val directory = File(context.filesDir,"books").apply { mkdirs() }

    fun initialize() {
        if (!settings.seeded) {
            db.save(DemoBook.create(directory))
            settings.seeded = true
        }
    }
    fun import(uri: Uri): Book {
        val temporary=File.createTempFile("import-", ".epub", directory)
        try {
            val digest=MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { input -> temporary.outputStream().use { out ->
                val buffer=ByteArray(8192); var total=0L
                while(true) { val count=input.read(buffer); if(count<0) break; total+=count; require(total<=Epub.MAX_BOOK_BYTES) { "El EPUB supera el límite de 100 MB." }; digest.update(buffer,0,count); out.write(buffer,0,count) }
            } } ?: error("No se pudo abrir el archivo. Selecciónalo de nuevo.")
            val id=digest.digest().joinToString("") { "%02x".format(it) }
            db.books().firstOrNull { it.id==id }?.let { return it }
            val parsed=Epub.parse(temporary,id)
            val target=File(directory,"$id.epub")
            check(temporary.renameTo(target)) { "No se pudo guardar el libro." }
            return parsed.copy(file=target.absolutePath).also { db.save(it) }
        } finally { temporary.delete() }
    }
    fun remove(book: Book) { db.deleteBook(book.id); File(book.file).delete() }
    suspend fun lookup(selection: Selection, action: String, p: Preferences, useAi: Boolean): Lookup {
        val cacheKey=AiClient.cacheKey(selection,if(useAi) action else "local-translate-v1",if(useAi) p else Preferences(endpoint="local",model="en-es"))
        db.cached(cacheKey)?.let { return it }
        return (if(useAi) AiClient.lookup(selection,action,p) else translation.lookup(selection)).also { db.cache(cacheKey,it) }
    }
    suspend fun definition(text: String, enabled: Boolean, language: String = "en"): DictionaryEntry? {
        val word=DictionaryClient.word(text) ?: return null
        val entry=db.dictionary(word,language) ?: if(enabled) DictionaryClient.fetch(word,language) else null
        entry ?: return null
        if(entry.definitionLanguage=="es") {db.cacheDictionary(word,entry);return entry}
        // Preserve the original dictionary text if the language pack is absent or translation fails.
        val ready=try {translation.ready()} catch(e: kotlinx.coroutines.CancellationException) {throw e} catch(_: Exception) {false}
        val translated=if(ready) entry.copy(senses=entry.senses.map { sense ->
            if(sense.spanish.isNotBlank()) sense else try {sense.copy(spanish=translation.definition(sense.definition))}
            catch(e: kotlinx.coroutines.CancellationException) {throw e}
            catch(_: Exception) {sense}
        }) else entry
        db.cacheDictionary(word,translated)
        return translated
    }
}
