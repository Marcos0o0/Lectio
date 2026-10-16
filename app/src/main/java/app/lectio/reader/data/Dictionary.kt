package app.lectio.reader.data

import android.content.Context
import org.json.JSONArray

class Dictionary(context: Context) {
    private val entries = context.assets.open("dictionary.json").bufferedReader().use { JSONArray(it.readText()) }.let { a ->
        (0 until a.length()).associate { i -> val j=a.getJSONObject(i); j.getString("word") to Lookup(j.getString("word"),j.getString("translation"),j.getString("definition"),j.getString("example"),j.optString("synonyms"),j.optString("level")) }
    }
    fun find(text: String): Lookup? {
        val word=text.trim().lowercase().trim { !it.isLetter() && it != '\'' }
        return entries[word]?.copy(original=text)
    }
    val size get() = entries.size
}
