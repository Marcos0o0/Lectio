package app.lectio.reader

import app.lectio.reader.data.*
import org.junit.Assert.*
import org.junit.Test

class LibraryCatalogTest {
    private val chapters=listOf(Chapter("one","Uno"))
    private fun book(id: String,title: String,author: String,added: Long,lastRead: Long=0)=Book(id,title,author,"file",null,chapters,added=added,lastRead=lastRead)
    private val books=listOf(book("z","Zorro","Ángela",30),book("a","Árboles","Zoé",10,50),book("n","Niños","Bruno",20))
    private fun ids(sort: LibrarySort,source: List<Book> = books)=LibraryCatalog.books(source,"",LibraryFilter.ALL,emptySet(),sort).map {it.id}

    @Test fun stableSpanishOrdersKeepReadingActivitySeparateFromImportDate() {
        assertEquals(listOf("a","z","n"),ids(LibrarySort.RECENT))
        assertEquals(listOf("z","n","a"),ids(LibrarySort.ADDED))
        assertEquals(listOf("a","n","z"),ids(LibrarySort.TITLE))
        assertEquals(listOf("z","n","a"),ids(LibrarySort.AUTHOR))
        val ties=books+books[1].copy(id="b")
        assertEquals(ids(LibrarySort.TITLE,ties),ids(LibrarySort.TITLE,ties.reversed()))
        assertEquals(LibrarySort.RECENT,LibrarySort.from("old-unknown-value"))
    }

    @Test fun searchCombinesWithFavoritesAndPreservesEnye() {
        fun search(query: String,filter: LibraryFilter=LibraryFilter.ALL)=LibraryCatalog.books(books,query,filter,setOf("a","n","deleted"),LibrarySort.TITLE).map {it.id}
        assertEquals(listOf("a"),search("  ARBOLES  ",LibraryFilter.FAVORITES))
        assertEquals(listOf("a"),search("Zoe\u0301"))
        assertEquals(listOf("n"),search("NIÑOS"))
        assertTrue(search("ninos").isEmpty())
        assertEquals(listOf("a","n"),search("",LibraryFilter.FAVORITES))
        assertTrue(search("Zorro",LibraryFilter.FAVORITES).isEmpty())
    }

    @Test fun statusFiltersRetainProgressAndFavoritesNeverChangeSourceBooks() {
        val source=listOf(books[0],books[1].copy(position=.5f),books[2].copy(position=1f,lastRead=60))
        assertEquals(listOf("a"),LibraryCatalog.books(source,"",LibraryFilter.READING,emptySet(),LibrarySort.TITLE).map {it.id})
        assertEquals(listOf("n"),LibraryCatalog.books(source,"",LibraryFilter.FINISHED,emptySet(),LibrarySort.TITLE).map {it.id})
        LibraryCatalog.books(source,"",LibraryFilter.FAVORITES,setOf("a"),LibrarySort.ADDED)
        assertEquals(.5f,source[1].position,0f)
        assertEquals(listOf("z","a","n"),source.map {it.id})
    }
}
