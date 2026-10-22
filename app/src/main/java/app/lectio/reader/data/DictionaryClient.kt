package app.lectio.reader.data

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

/** Only the selected word is sent to Wiktionary, never its sentence or book. */
internal object DictionaryClient {
    fun word(text: String): String? = Normalizer.normalize(text,Normalizer.Form.NFC).trim().trim('“','”','"','.',',','!','?',':',';','(',')','¿','¡','«','»','…')
        .replace('’','\'').lowercase(Locale.ROOT)
        .takeIf { it.length in 1..80 && Regex("[\\p{L}\\p{M}]+(?:['-][\\p{L}\\p{M}]+)*").matches(it) }

    fun fetch(word: String, language: String = "en"): DictionaryEntry? {
        require(this.word(word)==word)
        require(language in setOf("en","es"))
        val encoded=URLEncoder.encode(word,"UTF-8").replace("+","%20")
        if(language=="es") {
            val body=request("https://es.wiktionary.org/w/api.php?action=parse&prop=text&format=json&formatversion=2&redirects=1&page=$encoded") ?: return null
            val entry=parseSpanish(body,word) ?: run {
                val results=request("https://es.wiktionary.org/w/api.php?action=query&list=search&srnamespace=0&srlimit=5&format=json&srsearch=$encoded") ?: return null
                val title=accentVariant(results,word) ?: return null
                request("https://es.wiktionary.org/w/api.php?action=parse&prop=text&format=json&formatversion=2&redirects=1&page=${URLEncoder.encode(title,"UTF-8")}")?.let {parseSpanish(it,title)} ?: return null
            }
            if(entry.lemma.isBlank()||entry.lemma==word) return entry
            // Follow a documented inflection link once, never guess by removing suffixes.
            val base=runCatching {
                val lemma=URLEncoder.encode(entry.lemma,"UTF-8")
                request("https://es.wiktionary.org/w/api.php?action=parse&prop=text&format=json&formatversion=2&redirects=1&page=$lemma")?.let {parseSpanish(it,entry.lemma)}
            }.getOrNull() ?: return entry
            return entry.copy(senses=(base.senses+entry.senses.take(1)).distinctBy {it.definition}.take(5),sources=(entry.sources+base.sources).distinct(),relatedWords=base.relatedWords)
        }
        return request("https://en.wiktionary.org/api/rest_v1/page/definition/$encoded")?.let {parse(it,word)}
    }

    /** Accept only the same spelling with acute accents/diaeresis, never a fuzzy result or ñ → n. */
    fun accentVariant(body: String, selected: String): String? {
        fun fold(value: String)=Normalizer.normalize(value,Normalizer.Form.NFD).replace(Regex("[\u0301\u0308]"),"")
        val results=JSONObject(body).optJSONObject("query")?.optJSONArray("search") ?: return null
        return (0 until minOf(5,results.length())).mapNotNull {word(results.optJSONObject(it)?.optString("title").orEmpty())}
            .firstOrNull {it!=selected&&fold(it)==fold(selected)}
    }

    private fun request(url: String): String? {
        val connection=URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout=6000;connection.readTimeout=8000;connection.instanceFollowRedirects=false
            connection.setRequestProperty("Accept","application/json")
            connection.setRequestProperty("User-Agent","Lectio/0.5 (https://github.com/Marcos0o0/Lectio; Android reading app)")
            if(connection.responseCode==404) return null
            if(connection.responseCode!=200) throw IOException("Dictionary request failed")
            val bytes=connection.inputStream.use {input ->
                val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val n=input.read(buffer);if(n<0) break;if(output.size()+n>1_500_000) throw IOException("Dictionary response too large");output.write(buffer,0,n)}
                output.toByteArray()
            }
            return String(bytes,Charsets.UTF_8)
        } finally {connection.disconnect()}
    }

    fun parse(body: String, word: String): DictionaryEntry? {
        require(body.length<=512_000&&this.word(word)==word)
        val meanings=JSONObject(body).optJSONArray("en") ?: return null
        val senses=mutableListOf<WordSense>()
        for(i in 0 until minOf(meanings.length(),12)) {
            val meaning=meanings.optJSONObject(i) ?: continue
            val definitions=meaning.optJSONArray("definitions") ?: continue
            for(j in 0 until minOf(definitions.length(),2)) {
                val definition=definitions.optJSONObject(j) ?: continue
                val text=plain(definition.optString("definition"),900)
                val example=definition.optJSONArray("examples")?.optString(0).orEmpty()
                if(text.isNotBlank()&&senses.none {it.definition==text}&&senses.size<4)
                    senses.add(WordSense(meaning.optString("partOfSpeech").lowercase(Locale.ROOT).take(40),text,plain(example,500)))
            }
        }
        if(senses.isEmpty()) return null
        return DictionaryEntry(word,senses=senses,sources=listOf("https://en.wiktionary.org/wiki/${URLEncoder.encode(word,"UTF-8")}#English"),
            licenses=listOf(DictionaryCredit("CC BY-SA 4.0","https://creativecommons.org/licenses/by-sa/4.0/")))
    }

    fun parseSpanish(body: String, word: String): DictionaryEntry? {
        require(body.length<=1_500_000&&this.word(word)==word)
        val json=JSONObject(body)
        json.optJSONObject("error")?.let {if(it.optString("code")=="missingtitle") return null else throw IOException("Dictionary service error")}
        val html=json.optJSONObject("parse")?.optString("text") ?: return null
        val document=Jsoup.parse(html)
        var spanish=false;var category="";var lemma=""
        var hasLexicalMeaning=false
        val related=linkedSetOf<String>()
        val senses=mutableListOf<WordSense>()
        for(element in document.select("h2,h3,h4,dt")) {
            when(element.tagName()) {
                "h2" -> {spanish=element.id()=="Español"||element.selectFirst("#es")!=null;category=""}
                "h3","h4" -> if(spanish) {
                    val title=element.clone().apply {select(".mw-editsection").remove()}.text().trim()
                    category=title.takeIf {t -> listOf("Sustantivo","Adjetivo","Adverbio","Verbo","Pronombre","Preposición","Conjunción","Interjección","Artículo","Determinante","Forma","Participio","Gerundio","Locución").any {t.startsWith(it,true)}}.orEmpty()
                }
                "dt" -> if(spanish&&category.isNotBlank()&&Regex("^\\d+(?:\\s.*)?$").matches(element.text())) {
                    val dd=element.nextElementSibling()?.takeIf {it.tagName()=="dd"} ?: continue
                    val example=dd.select("li").firstOrNull {it.text().startsWith("Ejemplo",true)}?.text()?.substringAfter(':')?.trim().orEmpty().take(500)
                    val clean=dd.clone().apply {select("ul,ol,dl,table,script,style,sup,sub,.mw-editsection").remove()}.text().replace('\u00a0',' ').trim().take(900)
                    val inflection=category.startsWith("Forma",true)||category.startsWith("Participio",true)||category=="Gerundio"
                    if(clean.isNotBlank()&&!inflection) {hasLexicalMeaning=true;lemma=""}
                    // Related terms come from the definition, excluding examples and other languages.
                    dd.clone().apply {select("ul,ol,dl,table,sup,sub").remove()}.select("a[href^=/wiki/][title]").forEach {link ->
                        word(link.attr("title"))?.takeIf {it!=word}?.let {related.add(it)}
                    }
                    if(clean.isNotBlank()&&senses.none {it.definition==clean}&&senses.size<4) senses.add(WordSense(category,clean,example))
                    if(lemma.isBlank()&&inflection&&!hasLexicalMeaning) {
                        lemma=dd.clone().apply {select("ul,ol,dl,table,sup,sub").remove()}.select("a[href^=/wiki/][title]").lastOrNull()?.attr("title")?.let(::word).orEmpty()
                    }
                }
            }
        }
        if(senses.isEmpty()) return null
        return DictionaryEntry(word,senses=senses,sources=listOf("https://es.wiktionary.org/wiki/${URLEncoder.encode(word,"UTF-8")}#Español"),
            licenses=listOf(DictionaryCredit("CC BY-SA 4.0","https://creativecommons.org/licenses/by-sa/4.0/")),language="es",definitionLanguage="es",lemma=lemma,relatedWords=related.take(3))
    }
    private fun plain(html: String, limit: Int)=Jsoup.parse(html.take(10_000)).apply {select("script,style").remove()}.text().take(limit)
    fun safeLink(url: String): Boolean = runCatching {URI(url).let {
        it.scheme=="https"&&it.userInfo==null&&it.host in setOf("en.wiktionary.org","es.wiktionary.org","creativecommons.org")
    }}.getOrDefault(false)

    fun encode(entry: DictionaryEntry)=JSONObject().apply {
        put("word",entry.word);put("phonetic",entry.phonetic);put("language",entry.language);put("definitionLanguage",entry.definitionLanguage);put("lemma",entry.lemma)
        put("relatedWords",JSONArray(entry.relatedWords))
        put("senses",JSONArray().apply {entry.senses.forEach {put(JSONObject().put("part",it.partOfSpeech).put("definition",it.definition).put("example",it.example).put("spanish",it.spanish))}})
        put("sources",JSONArray(entry.sources));put("licenses",JSONArray().apply {entry.licenses.forEach {put(JSONObject().put("name",it.name).put("url",it.url))}})
    }
    fun decode(j: JSONObject): DictionaryEntry = DictionaryEntry(j.getString("word"),j.optString("phonetic"),
        j.getJSONArray("senses").let {a -> (0 until a.length()).map {a.getJSONObject(it).let {WordSense(it.getString("part"),it.getString("definition"),it.optString("example"),it.optString("spanish"))}}},
        j.optJSONArray("sources")?.let {a -> (0 until a.length()).map {a.getString(it)}.filter(::safeLink)}.orEmpty(),
        j.optJSONArray("licenses")?.let {a -> (0 until a.length()).map {a.getJSONObject(it).let {DictionaryCredit(it.getString("name"),it.getString("url"))}}.filter {safeLink(it.url)}}.orEmpty(),true,
        j.optString("language","en"),j.optString("definitionLanguage","en"),j.optString("lemma"),j.optJSONArray("relatedWords")?.let {a -> (0 until minOf(3,a.length())).mapNotNull {word(a.optString(it))}}.orEmpty())
}
