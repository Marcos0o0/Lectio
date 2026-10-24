package app.lectio.reader.data

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

enum class LibrarySort(val label: String) {
    RECENT("Recientes"), TITLE("Título"), AUTHOR("Autor"), ADDED("Últimos añadidos");

    companion object {
        fun from(value: String) = entries.firstOrNull {it.name==value} ?: RECENT
    }
}

enum class LibraryFilter(val label: String) {
    ALL("Todos"), FAVORITES("Favoritos"), READING("Leyendo"), FINISHED("Terminados")
}

/** Library presentation never changes a book's reading position or stored metadata. */
object LibraryCatalog {
    fun books(source: List<Book>, query: String, filter: LibraryFilter, favorites: Set<String>, sort: LibrarySort): List<Book> {
        val search=searchKey(query.trim())
        val collator=Collator.getInstance(Locale.forLanguageTag("es")).apply {strength=Collator.PRIMARY}
        val textOrder=Comparator<Book> {a,b ->
            val first=if(sort==LibrarySort.AUTHOR) collator.compare(a.author,b.author) else collator.compare(a.title,b.title)
            if(first!=0) first else {
                val second=if(sort==LibrarySort.AUTHOR) collator.compare(a.title,b.title) else collator.compare(a.author,b.author)
                if(second!=0) second else a.id.compareTo(b.id)
            }
        }
        val order=when(sort) {
            LibrarySort.RECENT -> compareByDescending<Book> {maxOf(it.lastRead,it.added)}.then(textOrder)
            LibrarySort.ADDED -> compareByDescending<Book> {it.added}.then(textOrder)
            else -> textOrder
        }
        return source.filter {book ->
            (searchKey(book.title).contains(search)||searchKey(book.author).contains(search))&&when(filter) {
                LibraryFilter.ALL -> true
                LibraryFilter.FAVORITES -> book.id in favorites
                LibraryFilter.READING -> book.lastRead>0&&book.progress<.995f
                LibraryFilter.FINISHED -> book.progress>=.995f
            }
        }.sortedWith(order)
    }

    // Ignore acute accents and diaeresis, but keep Spanish ñ distinct from n.
    private fun searchKey(value: String)=Normalizer.normalize(value.lowercase(Locale.ROOT),Normalizer.Form.NFD)
        .replace(Regex("[\u0301\u0308]"),"")
}
