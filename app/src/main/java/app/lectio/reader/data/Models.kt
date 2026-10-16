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
    val due: Long = 0, val interval: Int = 0, val source: String = "Diccionario local"
)
data class Annotation(val id: Long, val bookId: String, val chapter: Int, val position: Float,
    val text: String, val note: String, val type: String)
data class Selection(val text: String, val context: String = "")
data class Lookup(val original: String, val translation: String, val definition: String = "",
    val example: String = "", val synonyms: String = "", val level: String = "",
    val source: String = "Diccionario local", val cached: Boolean = false)
data class Preferences(val theme: String = "Sistema", val fontSize: Int = 20,
    val lineHeight: Float = 1.8f, val serif: Boolean = true, val endpoint: String = "https://api.openai.com/v1",
    val model: String = "gpt-4.1-mini", val key: String = "")
