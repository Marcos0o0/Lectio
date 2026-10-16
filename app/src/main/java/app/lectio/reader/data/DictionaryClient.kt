package app.lectio.reader.data

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

/** Only the selected word is sent to Wiktionary, never its sentence or book. */
internal object DictionaryClient {
    fun word(text: String): String? = text.trim().trim('“','”','"','.',',','!','?',':',';','(',')')
        .replace('’','\'').lowercase(Locale.ROOT)
        .takeIf { it.length in 1..80 && Regex("[a-z]+(?:['-][a-z]+)*").matches(it) }

    fun fetch(word: String): DictionaryEntry? {
        require(this.word(word)==word)
        val connection=URL("https://en.wiktionary.org/api/rest_v1/page/definition/$word").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout=6000;connection.readTimeout=8000;connection.instanceFollowRedirects=false
            connection.setRequestProperty("Accept","application/json")
            connection.setRequestProperty("User-Agent","Lectio/0.3 (https://github.com/Marcos0o0/Lectio; Android reading app)")
            if(connection.responseCode==404) return null
            if(connection.responseCode!=200) throw IOException("Dictionary request failed")
            val bytes=connection.inputStream.use {input ->
                val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true) {val n=input.read(buffer);if(n<0) break;if(output.size()+n>512_000) throw IOException("Dictionary response too large");output.write(buffer,0,n)}
                output.toByteArray()
            }
            return parse(String(bytes,Charsets.UTF_8),word)
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
        return DictionaryEntry(word,senses=senses,sources=listOf("https://en.wiktionary.org/wiki/$word#English"),
            licenses=listOf(DictionaryCredit("CC BY-SA 4.0","https://creativecommons.org/licenses/by-sa/4.0/")))
    }
    private fun plain(html: String, limit: Int)=Jsoup.parse(html.take(10_000)).apply {select("script,style").remove()}.text().take(limit)
    fun safeLink(url: String): Boolean = runCatching {URI(url).let {
        it.scheme=="https"&&it.userInfo==null&&it.host in setOf("en.wiktionary.org","creativecommons.org")
    }}.getOrDefault(false)

    fun encode(entry: DictionaryEntry)=JSONObject().apply {
        put("word",entry.word);put("phonetic",entry.phonetic)
        put("senses",JSONArray().apply {entry.senses.forEach {put(JSONObject().put("part",it.partOfSpeech).put("definition",it.definition).put("example",it.example).put("spanish",it.spanish))}})
        put("sources",JSONArray(entry.sources));put("licenses",JSONArray().apply {entry.licenses.forEach {put(JSONObject().put("name",it.name).put("url",it.url))}})
    }
    fun decode(j: JSONObject): DictionaryEntry = DictionaryEntry(j.getString("word"),j.optString("phonetic"),
        j.getJSONArray("senses").let {a -> (0 until a.length()).map {a.getJSONObject(it).let {WordSense(it.getString("part"),it.getString("definition"),it.optString("example"),it.optString("spanish"))}}},
        j.optJSONArray("sources")?.let {a -> (0 until a.length()).map {a.getString(it)}.filter(::safeLink)}.orEmpty(),
        j.optJSONArray("licenses")?.let {a -> (0 until a.length()).map {a.getJSONObject(it).let {DictionaryCredit(it.getString("name"),it.getString("url"))}}.filter {safeLink(it.url)}}.orEmpty(),true)
}
