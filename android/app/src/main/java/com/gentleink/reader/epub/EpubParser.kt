package com.gentleink.reader.epub

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

data class EpubChapter(
    val index: Int,
    val href: String,
    val title: String,
    val html: String
)

data class EpubBook(
    val title: String,
    val author: String?,
    val chapters: List<EpubChapter>
)

object EpubParser {
    fun parse(epubFile: File): EpubBook {
        ZipFile(epubFile).use { zip ->
            val containerEntry = zip.getEntry("META-INF/container.xml")
                ?: error("Invalid EPUB: missing container.xml")
            val containerXml = zip.getInputStream(containerEntry).bufferedReader().readText()
            val opfPath = extractOpfPath(containerXml)
            val opfEntry = zip.getEntry(opfPath.trimStart('/'))
                ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(".opf") }
                ?: error("Invalid EPUB: missing OPF")
            val opfXml = zip.getInputStream(opfEntry).bufferedReader().readText()
            val opfDir = opfEntry.name.substringBeforeLast('/', "")

            val (title, author) = parseMetadata(opfXml)
            val manifest = parseManifest(opfXml)
            val spine = parseSpine(opfXml)

            val chapters = spine.mapIndexed { index, href ->
                val resolved = resolveHref(opfDir, href)
                val entry = zip.getEntry(resolved)
                    ?: zip.entries().asSequence().firstOrNull { it.name.endsWith(href.substringAfterLast('/')) }
                    ?: error("Missing spine item: $href")
                val rawHtml = zip.getInputStream(entry).bufferedReader().readText()
                EpubChapter(
                    index = index,
                    href = href,
                    title = manifest[href]?.second ?: "Chapter ${index + 1}",
                    html = wrapHtml(rawHtml, title)
                )
            }

            return EpubBook(title = title, author = author, chapters = chapters)
        }
    }

    fun parseBytes(bytes: ByteArray, fallbackTitle: String = "Imported book"): EpubBook {
        val temp = File.createTempFile("gentleink", ".epub")
        try {
            temp.writeBytes(bytes)
            return parse(temp)
        } finally {
            temp.delete()
        }
    }

    private fun extractOpfPath(containerXml: String): String {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(containerXml.toByteArray()), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "rootfile") {
                return parser.getAttributeValue(null, "full-path") ?: error("Missing full-path")
            }
            event = parser.next()
        }
        error("OPF path not found")
    }

    private fun parseMetadata(opfXml: String): Pair<String, String?> {
        var title = "Untitled"
        var author: String? = null
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(opfXml.toByteArray()), "UTF-8")
        var inTitle = false
        var inCreator = false
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "dc:title", "title" -> inTitle = true
                    "dc:creator", "creator" -> inCreator = true
                }
                XmlPullParser.TEXT -> {
                    if (inTitle) title = parser.text.trim().ifBlank { title }
                    if (inCreator && author == null) author = parser.text.trim()
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "dc:title", "title" -> inTitle = false
                    "dc:creator", "creator" -> inCreator = false
                }
            }
            event = parser.next()
        }
        return title to author
    }

    private fun parseManifest(opfXml: String): Map<String, Pair<String, String>> {
        val map = mutableMapOf<String, Pair<String, String>>()
        val itemRegex = Regex("""<item[^>]+id="([^"]+)"[^>]+href="([^"]+)"[^>]*>""")
        val titleRegex = Regex("""<item[^>]+href="([^"]+)"[^>]+>[\s\S]*?<dc:title>([^<]+)</dc:title>""")
        for (match in itemRegex.findAll(opfXml)) {
            map[match.groupValues[2]] = match.groupValues[1] to match.groupValues[2]
        }
        return map
    }

    private fun parseSpine(opfXml: String): List<String> {
        val refs = mutableListOf<String>()
        val refRegex = Regex("""<itemref[^>]+idref="([^"]+)"[^>]*/>""")
        val idToHref = mutableMapOf<String, String>()
        val itemRegex = Regex("""<item[^>]+id="([^"]+)"[^>]+href="([^"]+)"[^>]*/>""")
        for (match in itemRegex.findAll(opfXml)) {
            idToHref[match.groupValues[1]] = match.groupValues[2]
        }
        for (match in refRegex.findAll(opfXml)) {
            idToHref[match.groupValues[1]]?.let { refs += it }
        }
        if (refs.isEmpty()) {
            refs += idToHref.values.toList()
        }
        return refs
    }

    private fun resolveHref(opfDir: String, href: String): String {
        val combined = if (opfDir.isBlank()) href else "$opfDir/$href"
        return combined.replace("\\", "/").replace(Regex("/+"), "/")
    }

    private fun wrapHtml(body: String, bookTitle: String): String {
        val content = if (body.contains("<html", ignoreCase = true)) body else """
            <!DOCTYPE html><html><head><meta charset="utf-8"/><title>$bookTitle</title></head><body>$body</body></html>
        """.trimIndent()
        return content
    }
}

object EpubFilterPipeline {
    fun filterBook(book: EpubBook, transform: (String) -> String): EpubBook {
        val chapters = book.chapters.map { chapter ->
            chapter.copy(html = EpubTextExtractor.filterHtml(chapter.html, transform))
        }
        return book.copy(chapters = chapters)
    }
}
