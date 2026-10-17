package app.lectio.reader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lectio.reader.data.*
import app.lectio.reader.data.Annotation
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class ReaderState(
    val loading: Boolean = true, val importing: Boolean = false,
    val books: List<Book> = emptyList(), val words: List<Word> = emptyList(),
    val annotations: List<Annotation> = emptyList(), val sessions: Map<String,Int> = emptyMap(),
    val preferences: Preferences = Preferences(), val current: Book? = null,
    val navigationId: Int = 0, val fragment: String? = null,
    val selection: Selection? = null, val lookup: Lookup? = null,
    val lookupLoading: Boolean = false, val lookupError: String? = null,
    val dictionary: DictionaryEntry? = null, val definitionLoading: Boolean = false, val definitionError: String? = null,
    val translationReady: Boolean = false, val translationDownloading: Boolean = false, val translationNeedsDownload: Boolean = false
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val repo=ReaderRepository(application)
    private val mutable=MutableStateFlow(ReaderState())
    val state=mutable.asStateFlow()
    private val messages=Channel<String>(Channel.BUFFERED)
    val events=messages.receiveAsFlow()
    private var lookupJob: Job? = null
    private var definitionJob: Job? = null
    private val preferenceWrites=Channel<Preferences>(Channel.UNLIMITED)
    private val progressWrites=Channel<Book>(Channel.UNLIMITED)
    init { viewModelScope.launch(Dispatchers.IO) { for(book in progressWrites) repo.db.save(book) } }
    init {viewModelScope.launch(Dispatchers.IO) {for(p in preferenceWrites) try {repo.settings.save(p)} catch(_:Exception) {message("No se pudieron guardar los ajustes. Vuelve a intentarlo.")}}}
    init { viewModelScope.launch {
        try { withContext(Dispatchers.IO) { repo.initialize(); refresh() } }
        catch(e: Exception) { message("No se pudo cargar la biblioteca: ${e.message}"); mutable.update { it.copy(loading=false) } }
    } }
    private fun refresh() { val books=repo.db.books();val words=repo.db.words();val sessions=repo.db.reading();val prefs=repo.settings.load(); mutable.update { it.copy(loading=false,books=books,words=words,sessions=sessions,preferences=if(it.loading) prefs else it.preferences) } }
    init { viewModelScope.launch { runCatching { repo.translation.ready() }.onSuccess {ready -> mutable.update {it.copy(translationReady=ready)}} } }
    fun message(text: String) { messages.trySend(text) }
    fun importBook(uri: Uri) { viewModelScope.launch {
        mutable.update { it.copy(importing=true) }
        try { withContext(Dispatchers.IO) { repo.import(uri);refresh() }; message("Libro añadido a tu biblioteca") }
        catch(e: Exception) { message(e.message ?: "No se pudo importar. Verifica que sea un EPUB sin DRM.") }
        finally { mutable.update { it.copy(importing=false) } }
    } }
    fun open(book: Book) { lookupJob?.cancel(); mutable.update { it.copy(current=book,selection=null,lookup=null,fragment=null,navigationId=it.navigationId+1) }; viewModelScope.launch(Dispatchers.IO) {
        val annotations=repo.db.annotations(book.id); mutable.update { if(it.current?.id==book.id) it.copy(annotations=annotations) else it }
    }; progress(book.chapter,book.position,book.textOffset) }
    fun closeBook() { dismissSelection(); mutable.update { it.copy(current=null) } }
    fun progress(chapter: Int, position: Float, textOffset: Int = -1) {
        val book=mutable.value.current ?: return
        val updated=book.copy(chapter=chapter.coerceIn(book.chapters.indices),position=position.coerceIn(0f,1f),textOffset=textOffset,lastRead=System.currentTimeMillis())
        mutable.update { it.copy(current=updated,books=it.books.map { b -> if(b.id==updated.id) updated else b }) }
        progressWrites.trySend(updated)
    }
    fun readerProgress(bookId: String, chapter: Int, position: Float, textOffset: Int = -1) { val current=mutable.value.current ?: return; if(current.id==bookId&&current.chapter==chapter) progress(chapter,position,textOffset) }
    fun chapter(index: Int, position: Float = 0f, fragment: String? = null, textOffset: Int = -1) { if(index !in (mutable.value.current?.chapters?.indices ?: return)) return;dismissSelection(); progress(index,position,textOffset);mutable.update { it.copy(navigationId=it.navigationId+1,fragment=fragment) } }
    fun navigationConsumed() {mutable.update {it.copy(fragment=null)}}
    fun remove(book: Book) { viewModelScope.launch(Dispatchers.IO) { repo.remove(book);refresh() }; message("Libro eliminado del dispositivo") }
    fun preferences(change: (Preferences) -> Preferences) {
        mutable.update {it.copy(preferences=change(it.preferences))}
        preferenceWrites.trySend(mutable.value.preferences)
    }
    fun select(selection: Selection) { if(selection.text.isBlank()||mutable.value.selection==selection) return; lookupJob?.cancel();definitionJob?.cancel(); mutable.update { it.copy(selection=selection,lookup=null,lookupError=null,lookupLoading=true,translationNeedsDownload=false,dictionary=null,definitionError=null,definitionLoading=false) }; query("translate",false);define() }
    fun define() {
        val selection=mutable.value.selection ?: return
        if(DictionaryClient.word(selection.text)==null) return
        definitionJob?.cancel()
        definitionJob=viewModelScope.launch {
            mutable.update {it.copy(definitionLoading=true,definitionError=null)}
            try {
                val entry=withContext(Dispatchers.IO) {repo.definition(selection.text,mutable.value.preferences.dictionaryEnabled)}
                ensureActive();mutable.update {it.copy(dictionary=entry,definitionLoading=false)}
            } catch(e: CancellationException) {throw e}
            catch(_: Exception) {mutable.update {it.copy(definitionLoading=false,definitionError="No se pudo consultar el diccionario. Revisa tu conexión y vuelve a intentarlo.")}}
        }
    }
    fun downloadTranslation(wifiOnly: Boolean = true) {
        if(mutable.value.translationDownloading) return
        viewModelScope.launch {
            mutable.update {it.copy(translationDownloading=true,lookupError=null)}
            try {
                repo.translation.download(wifiOnly)
                mutable.update {it.copy(translationReady=true,translationNeedsDownload=false,translationDownloading=false)}
                if(mutable.value.selection!=null) {query("translate",false);define()}
                message("Español descargado. Ya puedes traducir sin conexión.")
            } catch(e: CancellationException) {throw e}
            catch(e: Exception) {val error="No se pudo descargar el idioma. Revisa tu conexión y vuelve a intentarlo.";mutable.update {it.copy(translationDownloading=false,lookupError=error)};message(error)}
        }
    }
    fun query(action: String, useAi: Boolean = true) {
        val selection=mutable.value.selection ?: return
        lookupJob?.cancel()
        lookupJob=viewModelScope.launch {
            mutable.update { it.copy(lookupLoading=true,lookupError=null,translationNeedsDownload=false) }
            try {
                val result=withContext(Dispatchers.IO) { repo.lookup(selection,action,mutable.value.preferences,useAi) }
                ensureActive(); mutable.update { it.copy(lookup=result,lookupLoading=false) }
            } catch(e: CancellationException) { throw e }
            catch(e: TranslationModelRequired) {mutable.update {it.copy(lookupLoading=false,translationNeedsDownload=true)}}
            catch(e:Exception) { mutable.update { it.copy(lookupError=if(e is java.io.IOException) "No se pudo conectar. Revisa tu conexión e intenta de nuevo." else e.message ?: "No se pudo consultar el fragmento.",lookupLoading=false) } }
        }
    }
    fun dismissSelection() { lookupJob?.cancel();definitionJob?.cancel(); mutable.update { it.copy(selection=null,lookup=null,lookupLoading=false,lookupError=null,translationNeedsDownload=false,dictionary=null,definitionLoading=false,definitionError=null) } }
    override fun onCleared() {repo.translation.close();repo.db.close();super.onCleared()}
    fun saveWord(manual: String? = null) {
        val s=mutable.value;val selection=s.selection ?: return; val lookup=s.lookup
        if(selection.text.trim().split(Regex("\\s+")).size>12) { message("Para tu vocabulario, selecciona una palabra o expresión corta.");return }
        val translation=manual?.trim()?.takeIf { it.isNotEmpty() } ?: lookup?.translation ?: return
        val existing=s.words.firstOrNull {it.text.equals(selection.text.trim(),true)}
        val sense=s.dictionary?.senses?.firstOrNull()
        val definition=lookup?.definition?.takeIf {it.isNotBlank()} ?: sense?.let {it.spanish.ifBlank {it.definition}} ?: existing?.definition.orEmpty()
        val fromDictionary=lookup?.definition.isNullOrBlank()&&sense!=null
        val aiDefinition=lookup?.definition?.isNotBlank()==true
        val example=lookup?.example?.takeIf {it.isNotBlank()} ?: if(fromDictionary) sense?.example.orEmpty() else if(aiDefinition) "" else existing?.example.orEmpty()
        val word=Word(selection.text.trim(),translation,definition,example,lookup?.synonyms.orEmpty(),lookup?.level.orEmpty(),s.current?.title.orEmpty(),selection.context,learned=existing?.learned ?: false,due=existing?.due ?: 0,interval=existing?.interval ?: 0,source=if(manual!=null) "Traducción propia" else lookup?.source.orEmpty(),dictionarySource=if(fromDictionary) s.dictionary?.sources?.firstOrNull().orEmpty() else if(aiDefinition) "" else existing?.dictionarySource.orEmpty(),dictionaryLicense=if(fromDictionary) s.dictionary?.licenses?.firstOrNull()?.url.orEmpty() else if(aiDefinition) "" else existing?.dictionaryLicense.orEmpty())
        viewModelScope.launch(Dispatchers.IO) { repo.db.save(word); refresh() };message("Guardado en tu vocabulario")
    }
    fun learn(word: Word, correct: Boolean) { viewModelScope.launch(Dispatchers.IO) {
        val days=if(!correct) 0 else when(word.interval) { 0->1;1->3;else->(word.interval*2).coerceAtMost(180) }
        repo.db.save(word.copy(interval=days,due=System.currentTimeMillis()+if(correct) days*86400000L else 600000L,learned=days>=7));refresh()
    } }
    fun toggleLearned(word: Word) { viewModelScope.launch(Dispatchers.IO) { repo.db.save(word.copy(learned=!word.learned));refresh() } }
    fun deleteWord(word: Word) { viewModelScope.launch(Dispatchers.IO) { repo.db.deleteWord(word.text);refresh() } }
    fun annotate(text: String = "", note: String = "", type: String = "bookmark") {
        val book=mutable.value.current ?: return
        viewModelScope.launch(Dispatchers.IO) { repo.db.annotate(book.id,book.chapter,book.position,text,note,type,book.textOffset); val annotations=repo.db.annotations(book.id); mutable.update { if(it.current?.id==book.id) it.copy(annotations=annotations) else it } }
        message(if(type=="bookmark") "Marcador guardado" else "Subrayado guardado")
    }
    fun deleteAnnotation(id: Long) { val book=mutable.value.current ?: return; viewModelScope.launch(Dispatchers.IO) { repo.db.deleteAnnotation(id); val annotations=repo.db.annotations(book.id);mutable.update { it.copy(annotations=annotations) } } }
    fun readingTick(seconds: Int) { viewModelScope.launch(Dispatchers.IO) { repo.db.addReading(seconds);val sessions=repo.db.reading();mutable.update { it.copy(sessions=sessions) } } }
}
