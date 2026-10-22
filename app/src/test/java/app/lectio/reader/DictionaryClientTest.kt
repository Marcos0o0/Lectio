package app.lectio.reader

import app.lectio.reader.data.DictionaryClient
import org.junit.Assert.*
import org.junit.Test

class DictionaryClientTest {
    @Test fun onlyASingleUnicodeWordCanBecomeAnApiPath() {
        assertEquals("precious",DictionaryClient.word("“Precious!”"))
        assertEquals("don't",DictionaryClient.word("don’t"))
        assertEquals("árbol",DictionaryClient.word("¡A\u0301rbol!"))
        assertEquals("niños",DictionaryClient.word("Niños"))
        listOf("a whole sentence","../secret","word?token=secret","https://example.com","", "a".repeat(81)).forEach {assertNull(DictionaryClient.word(it))}
    }
    @Test fun realSpanishEntriesExplainTheUsersWordsAndLinkInflections() {
        fun entry(file: String, word: String)=DictionaryClient.parseSpanish(javaClass.getResource("/dictionary-es/$file.json")!!.readText(),word)!!
        val patriarcado=entry("patriarcado","patriarcado")
        assertTrue(patriarcado.senses.first().definition.contains("Sistema social"))
        assertEquals("es",patriarcado.definitionLanguage)
        val feminista=entry("feminista","feminista")
        assertTrue(feminista.senses.first().definition.contains("feminismo"))
        assertTrue(feminista.relatedWords.contains("feminismo"))
        assertFalse(feminista.senses.any {it.definition.contains("hispana")})
        assertEquals("ensombrecer",entry("ensombrecieron","ensombrecieron").lemma)
        assertEquals("metalingüístico",entry("metalinguisticas","metalingüísticas").lemma)
        assertEquals("sesgo",entry("sesgos","sesgos").lemma)
        assertTrue(entry("despolitizacion","despolitización").senses.first().definition.contains("carga ideológica"))
        assertEquals(patriarcado.copy(cached=true),DictionaryClient.decode(DictionaryClient.encode(patriarcado)))
    }
    @Test fun accentsAreResolvedOnlyWhenTheProviderHasTheSameSpelling() {
        val results="""{"query":{"search":[{"title":"despolitizaciones"},{"title":"despolitización"}]}}"""
        assertEquals("despolitización",DictionaryClient.accentVariant(results,"despolitizacion"))
        assertNull(DictionaryClient.accentVariant("""{"query":{"search":[{"title":"año"}]}}""","ano"))
        assertNull(DictionaryClient.accentVariant(results,"desconocido"))
    }
    @Test fun spanishParserExcludesOtherLanguagesExamplesAndMissingPages() {
        val html="""<h2 id="Español"></h2><h3>Adjetivo</h3><dl><dt>1</dt><dd>Un significado.<ul><li>Ejemplo: Una oración.</li></ul></dd></dl><h2>Portugués</h2><h3>Adjetivo</h3><dl><dt>1</dt><dd>Another language.</dd></dl>"""
        val body=org.json.JSONObject().put("parse",org.json.JSONObject().put("text",html)).toString()
        val sense=DictionaryClient.parseSpanish(body,"palabra")!!.senses.single()
        assertEquals("Un significado.",sense.definition)
        assertEquals("Una oración.",sense.example)
        val mixed=html.replace("<h2>Portugués</h2>","""<h3>Forma verbal</h3><dl><dt>2</dt><dd>Forma de <a href="/wiki/palabrar" title="palabrar">palabrar</a>.</dd></dl><h2>Portugués</h2>""")
        val mixedBody=org.json.JSONObject().put("parse",org.json.JSONObject().put("text",mixed)).toString()
        // A homograph with its own lexical meaning must not be replaced by a verb lemma.
        assertEquals("",DictionaryClient.parseSpanish(mixedBody,"palabra")!!.lemma)
        assertNull(DictionaryClient.parseSpanish("""{"error":{"code":"missingtitle"}}""","palabra"))
        assertTrue(DictionaryClient.safeLink("https://es.wiktionary.org/wiki/patriarcado"))
        assertFalse(DictionaryClient.safeLink("https://es.wiktionary.org.evil.test/wiki/word"))
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
