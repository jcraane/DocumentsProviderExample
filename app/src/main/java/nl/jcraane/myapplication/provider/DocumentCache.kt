package nl.jcraane.myapplication.provider

import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class DocumentCache {
    private val cache: ConcurrentMap<Uri, List<DocumentMetaData>> = ConcurrentHashMap()

    fun add(uri: Uri, documents: List<DocumentMetaData>) {
        cache.put(uri, documents)
    }

    fun get(uri: Uri) = cache.get(uri)
}

data class DocumentMetaData(
    val id: String,
    val mimeType: String,
    val displayName: String,
    val lastModified: Long,
    val flags: Int,
    val size: Long
) {
    fun addToRow(row: MatrixCursor.RowBuilder) {
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, id)
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, mimeType)
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, displayName)
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, lastModified)
        row.add(DocumentsContract.Document.COLUMN_FLAGS, flags)
        row.add(DocumentsContract.Document.COLUMN_SIZE, size)
    }
}