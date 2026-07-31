package com.gentleink.reader.epub

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

object EpubTextExtractor {
    fun extractPlainText(epubBytes: ByteArray): String {
        val htmlParts = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(epubBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && (
                        entry.name.endsWith(".xhtml", ignoreCase = true) ||
                            entry.name.endsWith(".html", ignoreCase = true)
                        )
                ) {
                    val html = zip.readBytes().decodeToString()
                    htmlParts += stripHtml(html)
                }
                entry = zip.nextEntry
            }
        }
        return htmlParts.joinToString("\n\n") { it.trim() }.trim()
    }

    fun filterHtml(html: String, transform: (String) -> String): String {
        val textNodes = mutableListOf<Pair<IntRange, String>>()
        val pattern = Regex(">([^<]+)<")
        pattern.findAll(html).forEach { match ->
            val content = match.groupValues[1]
            if (content.isNotBlank() && content.any { it.isLetter() }) {
                textNodes += match.groups[1]!!.range to content
            }
        }

        if (textNodes.isEmpty()) return html

        var result = html
        for ((range, content) in textNodes.sortedByDescending { it.first.first }) {
            val filtered = transform(content)
            if (filtered != content) {
                result = result.replaceRange(range, filtered)
            }
        }
        return result
    }

    private fun stripHtml(html: String): String {
        return html
            .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
