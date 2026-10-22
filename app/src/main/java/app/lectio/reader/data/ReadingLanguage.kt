package app.lectio.reader.data

import java.text.Normalizer
import java.util.Locale

/** EPUB metadata first. A small language hint is only a fallback, never a dictionary. */
internal object ReadingLanguage {
    fun code(tag: String): String? = tag.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_').takeIf {it=="en"||it=="es"}
    fun resolve(book: Book?, selection: Selection): String {
        code(book?.dictionaryLanguage.orEmpty())?.let {return it}
        code(book?.language.orEmpty())?.let {return it}
        val text=Normalizer.normalize(selection.context.ifBlank {selection.text},Normalizer.Form.NFC).lowercase(Locale.ROOT)
        if(Regex("[áéíóúüñ¿¡]").containsMatchIn(text)) return "es"
        val words=Regex("\\p{L}+").findAll(text).map {it.value}.toList()
        val spanish=setOf("el","la","los","las","un","una","de","del","que","en","con","por","para","pero","sus","como","estaba","muy","se")
        val english=setOf("the","a","an","of","that","in","with","for","but","was","were","and","this","is","are","his","her")
        return if(words.count {it in spanish}>words.count {it in english}) "es" else "en"
    }
}
