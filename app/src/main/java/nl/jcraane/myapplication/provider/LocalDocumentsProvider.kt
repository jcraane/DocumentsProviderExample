package nl.jcraane.myapplication.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import nl.jcraane.myapplication.R
import nl.jcraane.myapplication.provider.ProdiverConfig.DEFAULT_DOCUMENT_PROJECTION
import nl.jcraane.myapplication.provider.ProdiverConfig.DEFAULT_ROOT_PROJECTION
import nl.jcraane.myapplication.provider.ProdiverConfig.ROOT_FOLDER_ID
import nl.jcraane.myapplication.provider.ProdiverConfig.ROOT_ID
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class LocalDocumentsProvider : DocumentsProvider() {
    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val descriptor = context?.resources?.openRawResource(R.raw.sample)?.use { input ->
            val file = File(context?.getCacheDir(), "cacheFileAppeal.srl")
            FileOutputStream(file).use({ output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int = 0
                while (input.read(buffer).also({ read = it }) != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            })
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode));
        }

        if (descriptor == null) {
            throw IllegalArgumentException("Null descriptor")
        }
        return descriptor
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        Timber.i("queryChildDocuments(parentDocumentId, projection, sortOrder)($parentDocumentId, $projection, $sortOrder)")
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)

        if (parentDocumentId == ROOT_FOLDER_ID) {
            for (i in 0..10) {
                val row = cursor.newRow()
                with(row) {
                    add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, i.toShort())
                    add(DocumentsContract.Document.COLUMN_MIME_TYPE, if (i == 0) DocumentsContract.Document.MIME_TYPE_DIR else "text/plain")
                    add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "name$i")
                    add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0)
                    add(DocumentsContract.Document.COLUMN_FLAGS, 0)
                    add(DocumentsContract.Document.COLUMN_SIZE, 10)
                }

            }
        } else {
            for (i in 0..2) {
                val row = cursor.newRow()
                with(row) {
                    add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, i.toShort())
                    add(DocumentsContract.Document.COLUMN_MIME_TYPE, "text/plain")
                    add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "Child $i")
                    add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0)
                    add(DocumentsContract.Document.COLUMN_FLAGS, 0)
                    add(DocumentsContract.Document.COLUMN_SIZE, 10)
                }

            }
        }

        return cursor
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        Timber.i("queryDocument(documentId, projection)($documentId,$projection)")
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        if (documentId == ROOT_FOLDER_ID) {
            val row = cursor.newRow()
            with(row) {
                add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, ROOT_FOLDER_ID)
                add(DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.MIME_TYPE_DIR)
                add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, context?.getString(R.string.app_name) ?: "DocumentsProviderExample")
                add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, null)
                add(DocumentsContract.Document.COLUMN_FLAGS, 0)
                add(DocumentsContract.Document.COLUMN_SIZE, null)
            }
        }
        return cursor
    }

    override fun onCreate(): Boolean {
        Timber.i("onCreate")
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        Timber.i("queryRoots")
        return MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
            .apply {
                val row = newRow()
                with(row) {
                    add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID)
                    add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_launcher_foreground)
                    add(
                        DocumentsContract.Root.COLUMN_TITLE,
                        context?.getString(R.string.app_name) ?: "DocumentsProviderExample"
                    )
                    add(DocumentsContract.Root.COLUMN_FLAGS, 0)
                    add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_FOLDER_ID)
                }
            }
    }
}