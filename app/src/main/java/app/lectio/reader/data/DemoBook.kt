package app.lectio.reader.data

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DemoBook {
    fun create(directory: File): Book {
        val file=File(directory,"a-little-more-slowly.epub")
        val chapters=listOf(
            "The quiet between things" to """
                <p>There is a particular kind of quiet that arrives before the rest of the world is awake. It belongs neither to the night nor to the morning. For a few precious minutes, nothing is expected of you.</p>
                <p>I discovered this quiet by accident. A train I needed to catch was cancelled, and I found myself sitting on a wooden bench outside a small station. My phone had run out of battery. There was nowhere to go and nothing to do but notice.</p>
                <p>Across the road, a woman was opening a bakery. She carried a tray of bread with the careful attention that people usually reserve for something fragile. A small cloud of flour drifted from her apron. She looked up, saw me, and smiled.</p>
                <p>I had passed that bakery perhaps a hundred times. I had never noticed the blue door, the uneven step, or the rosemary growing in a pot beside the entrance. I had been too busy getting somewhere else.</p>
                <h2>A different kind of attention</h2>
                <p>We tend to believe that paying attention requires effort. We narrow our eyes, lean forward, and try to concentrate. But there is another kind of attention, one that begins when we stop trying to make the moment useful.</p>
                <p>The morning at the station taught me very little that I could explain. Yet something shifted. The ordinary world, which had seemed so familiar, became quietly remarkable.</p>
                <p>Perhaps this is what it means to be present: not to understand everything, but to leave enough room for something unexpected. To let a familiar street become unfamiliar again. To notice the light before reaching for the switch.</p>
                <p>I missed my meeting that day. When a friend asked whether the journey had been a waste of time, I thought about the bread, the blue door, and the woman who smiled. I wasn't sure how to answer.</p>
            """.trimIndent(),
            "A word worth keeping" to """
                <p>On my desk there is a notebook full of words. Some are practical. Some are beautiful. A few are there simply because I encountered them on a day I wanted to remember.</p>
                <p>The first word I wrote was <em>linger</em>. I liked the way it seemed to do what it described: to stay a little longer than necessary, reluctant to leave. We linger over a good meal. A scent lingers in an empty room. A sentence lingers in the mind long after the book is closed.</p>
                <p>Learning another language has changed the way I read. Once, I hurried past anything I didn't understand. Now I pause. I look at the words around the unfamiliar word. I ask what the sentence would lose without it.</p>
                <h2>Learning in the margins</h2>
                <p>A dictionary can tell you what a word means. A story shows you what it can do. The difference is subtle, but it matters. We remember things more easily when they belong to something we care about.</p>
                <p>This is why I keep the whole sentence, not just the word. Beside <em>linger</em> I wrote: “She lingered at the doorway, hoping he would ask her to stay.” The sentence carries a small world inside it. I can return to that world whenever I need the word.</p>
                <p>There is no need to collect everything. A single word, understood well, is enough for one day. Tomorrow it might appear again, in a different book or a conversation, and you will recognise an old friend.</p>
            """.trimIndent(),
            "The long way home" to """
                <p>At the end of the afternoon, I took the long way home. It was not a particularly sensible decision. There were messages waiting, a kitchen to clean, and a list of things I had promised myself I would finish.</p>
                <p>But the path beside the river was full of evening light. A child was teaching a dog to sit. The dog was teaching the child to be patient. Neither of them seemed to be making much progress, and both looked happy.</p>
                <p>I thought about how often we measure a day by what we have completed. The unanswered messages become evidence of failure. The unfinished book becomes another obligation. Even rest becomes something to do correctly.</p>
                <h2>Enough for today</h2>
                <p>What if we measured differently? A chapter read with curiosity. A conversation we didn't rush. A word we understood for the first time. These things may not look impressive on a list, but they are part of a life.</p>
                <p>I stopped on the bridge. Below me, the water carried a small branch towards the sea. It turned slowly, caught for a moment in a patch of sunlight, and continued. There was something reassuring about its unhurried journey.</p>
                <p>When I finally reached home, the messages were still waiting. The kitchen was still a mess. Nothing had been solved. Yet I opened the door with a feeling I had almost forgotten: I was glad to be exactly where I was.</p>
            """.trimIndent()
        )
        ZipOutputStream(file.outputStream()).use { zip ->
            fun entry(path:String,text:String) { zip.putNextEntry(ZipEntry(path));zip.write(text.toByteArray());zip.closeEntry() }
            entry("mimetype","application/epub+zip")
            entry("META-INF/container.xml","""<container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            entry("OEBPS/content.opf","""<package xmlns="http://www.idpf.org/2007/opf" version="3.0" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">lectio-demo</dc:identifier><dc:title>A Little More Slowly</dc:title><dc:creator>Lectio · lectura de muestra</dc:creator><dc:language>en</dc:language></metadata><manifest>${chapters.indices.joinToString("") { "<item id=\"c$it\" href=\"chapter$it.xhtml\" media-type=\"application/xhtml+xml\"/>" }}</manifest><spine>${chapters.indices.joinToString("") { "<itemref idref=\"c$it\"/>" }}</spine></package>""")
            chapters.forEachIndexed { i,(title,body) -> entry("OEBPS/chapter$i.xhtml","""<html xmlns="http://www.w3.org/1999/xhtml"><head><title>$title</title></head><body><h1>$title</h1>$body</body></html>""") }
        }
        return Epub.parse(file,"lectio-demo").copy(demo=true)
    }
}
