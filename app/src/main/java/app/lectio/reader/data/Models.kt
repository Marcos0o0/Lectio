package app.lectio.reader.data

data class Chapter(val path: String, val title: String)
data class Book(
    val id: String, val title: String, val author: String, val file: String,
    val cover: String?, val chapters: List<Chapter>, val chapter: Int = 0,
    val position: Float = 0f, val lastRead: Long = 0L, val added: Long = System.currentTimeMillis(),
    val demo: Boolean = false
) {
    val progress: Float get() = if (chapters.isEmpty()) 0f else ((chapter + position) / chapters.size).coerceIn(0f, 1f)
}
data class Word(
    val text: String, val translation: String, val definition: String = "",
    val example: String = "", val synonyms: String = "", val level: String = "",
    val book: String = "", val context: String = "", val learned: Boolean = false,
    val due: Long = 0, val interval: Int = 0, val source: String = "Diccionario local",
    val dictionarySource: String = "", val dictionaryLicense: String = ""
)
data class Annotation(val id: Long, val bookId: String, val chapter: Int, val position: Float,
    val text: String, val note: String, val type: String)
data class Selection(val text: String, val context: String = "")
data class Lookup(val original: String, val translation: String, val definition: String = "",
    val example: String = "", val synonyms: String = "", val level: String = "",
    val source: String = "Google Translate · en el dispositivo", val cached: Boolean = false,
    val contextTranslation: String = "")
data class Preferences(val theme: String = "Oscuro", val fontSize: Int = 20,
    val lineHeight: Float = 1.8f, val serif: Boolean = true, val endpoint: String = "https://generativelanguage.googleapis.com/v1beta",
    val model: String = "gemini-3.5-flash-lite", val key: String = "", val provider: String = "gemini",
    val readingTheme: String = "Aplicación", val margin: Int = 28,
    val paragraphSpacing: Float = 1f, val justified: Boolean = false,
    val keepScreenOn: Boolean = false, val tapToTranslate: Boolean = true,
    val showReadingHint: Boolean = true, val dictionaryEnabled: Boolean = true)

data class WordSense(val partOfSpeech: String, val definition: String, val example: String = "", val spanish: String = "")
data class DictionaryCredit(val name: String, val url: String)
data class DictionaryEntry(val word: String, val phonetic: String = "", val senses: List<WordSense>,
    val sources: List<String> = emptyList(), val licenses: List<DictionaryCredit> = emptyList(), val cached: Boolean = false)
