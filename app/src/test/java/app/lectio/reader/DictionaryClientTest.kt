package app.lectio.reader

import app.lectio.reader.data.DictionaryClient
import org.junit.Assert.*
import org.junit.Test

class DictionaryClientTest {
    @Test fun onlyASingleEnglishWordCanBecomeAnApiPath() {
        assertEquals("precious",DictionaryClient.word("“Precious!”"))
        assertEquals("don't",DictionaryClient.word("don’t"))
        listOf("a whole sentence","../secret","word?token=secret","https://example.com","", "a".repeat(81)).forEach {assertNull(DictionaryClient.word(it))}
    }
    @Test fun dictionaryKeepsOriginalDefinitionsAndCreditsAcrossCache() {
        val entry=DictionaryClient.parse("""{"en":[{"partOfSpeech":"Adjective","definitions":[{"definition":"Of high <a href='/wiki/value'>value</a> or worth.","examples":["A <b>precious</b> jewel."]}]}]}""","precious")!!
        assertEquals("adjective",entry.senses.single().partOfSpeech)
        assertEquals("A precious jewel.",entry.senses.single().example)
        assertEquals(1,entry.sources.size)
        val cached=DictionaryClient.decode(DictionaryClient.encode(entry.copy(senses=entry.senses.map {it.copy(spanish="De gran valor.")})))
        assertTrue(cached.cached)
        assertEquals("Of high value or worth.",cached.senses.single().definition)
        assertEquals("De gran valor.",cached.senses.single().spanish)
        assertEquals(entry.licenses,cached.licenses)
    }
    @Test fun definitionsAreBoundedAndNoEntryIsRepresentedExplicitly() {
        assertNull(DictionaryClient.parse("{}","word"))
        assertNull(DictionaryClient.parse("""{"en":[]}""","word"))
        val definitions=(1..10).joinToString(",") {"""{"definition":"Meaning $it"}"""}
        val meanings=(1..10).joinToString(",") {"""{"partOfSpeech":"Noun","definitions":[$definitions]}"""}
        val entry=DictionaryClient.parse("""{"en":[$meanings]}""","word")!!
        assertTrue(entry.senses.size<=4)
        assertFalse(DictionaryClient.safeLink("https://en.wiktionary.org.evil.test/wiki/word"))
        assertFalse(DictionaryClient.safeLink("https://user@en.wiktionary.org/wiki/word"))
    }
}
