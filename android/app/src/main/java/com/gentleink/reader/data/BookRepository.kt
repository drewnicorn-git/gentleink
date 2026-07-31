package com.gentleink.reader.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class LibraryBook(
    val id: String,
    val title: String,
    val author: String?,
    val fileName: String,
    val addedAt: Long
)

class BookRepository(context: Context) {
    private val booksDir = File(context.filesDir, "books").apply { mkdirs() }
    private val indexFile = File(context.filesDir, "library.json")

    fun listBooks(): List<LibraryBook> {
        if (!indexFile.exists()) return emptyList()
        val array = JSONArray(indexFile.readText())
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            LibraryBook(
                id = obj.getString("id"),
                title = obj.getString("title"),
                author = obj.optString("author").ifBlank { null },
                fileName = obj.getString("fileName"),
                addedAt = obj.getLong("addedAt")
            )
        }.sortedByDescending { it.addedAt }
    }

    fun bookFile(id: String): File? {
        val book = listBooks().firstOrNull { it.id == id } ?: return null
        return File(booksDir, book.fileName)
    }

    fun importBook(sourceBytes: ByteArray, originalName: String, title: String, author: String?): LibraryBook {
        val id = UUID.randomUUID().toString()
        val safeName = "$id.epub"
        File(booksDir, safeName).writeBytes(sourceBytes)
        val entry = LibraryBook(
            id = id,
            title = title,
            author = author,
            fileName = safeName,
            addedAt = System.currentTimeMillis()
        )
        saveEntry(entry)
        return entry
    }

    fun deleteBook(id: String) {
        val books = listBooks().filter { it.id != id }
        listBooks().firstOrNull { it.id == id }?.fileName?.let { File(booksDir, it).delete() }
        writeIndex(books)
    }

    private fun saveEntry(entry: LibraryBook) {
        writeIndex(listBooks() + entry)
    }

    private fun writeIndex(books: List<LibraryBook>) {
        val array = JSONArray()
        books.distinctBy { it.id }.forEach { book ->
            array.put(
                JSONObject()
                    .put("id", book.id)
                    .put("title", book.title)
                    .put("author", book.author ?: "")
                    .put("fileName", book.fileName)
                    .put("addedAt", book.addedAt)
            )
        }
        indexFile.writeText(array.toString(2))
    }
}
