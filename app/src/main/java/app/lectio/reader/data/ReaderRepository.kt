package app.lectio.reader.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest

class ReaderRepository(private val context: Context) {
    val db = LibraryDb(context)
    val settings = SettingsStore(context)
    val dictionary by lazy { Dictionary(context) }
    private val directory = File(context.filesDir,"books").apply { mkdirs() }

    fun initialize() {
        if (!settings.seeded) {
            db.save(DemoBook.create(directory))
            settings.seeded = true
        }
        dictionary.size
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
    fun lookup(selection: Selection, action: String, p: Preferences, useAi: Boolean): Lookup {
        if (action=="translate" && !useAi) dictionary.find(selection.text)?.let { return it }
        if (action=="translate" && !useAi) db.words().firstOrNull { it.text.equals(selection.text.trim(),true) }?.let {
            return Lookup(selection.text,it.translation,it.definition,it.example,it.synonyms,it.level,it.source,true)
        }
        val cacheKey=AiClient.cacheKey(selection,action,p)
        db.cached(cacheKey)?.let { return it }
        if(!useAi) error("No está en el diccionario inicial. Puedes consultar la IA con tu clave, o guardar una traducción propia.")
        return AiClient.lookup(selection,action,p).also { db.cache(cacheKey,it) }
    }
}
