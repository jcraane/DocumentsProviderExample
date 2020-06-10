package nl.jcraane.myapplication.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.jcraane.myapplication.BuildConfig
import nl.jcraane.myapplication.R
import nl.jcraane.myapplication.provider.ProviderConfig.DEFAULT_DOCUMENT_PROJECTION
import nl.jcraane.myapplication.provider.ProviderConfig.DEFAULT_ROOT_PROJECTION
import nl.jcraane.myapplication.provider.ProviderConfig.ROOT_FOLDER_ID
import nl.jcraane.myapplication.provider.ProviderConfig.ROOT_ID
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class WriteFileTask(
    private val out: ParcelFileDescriptor,
    private val buffer: ByteArray
) : AsyncTask<Unit, Unit, Unit>() {

    override fun doInBackground(vararg params: Unit?) {
        println("In background")
        ParcelFileDescriptor.AutoCloseOutputStream(out).use {
            it.write(buffer)
            it.flush()
        }
    }
}

class SimulateNetworkDocumentsProvider : DocumentsProvider() {
    private val cache = DocumentCache()

    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor {
        Timber.i("openDpcument(documentId,mode,signal)($documentId,$mode,$signal)")
//        this one works with Gmail
        return readFileUsingOpenDocument(documentId, mode)
//        this one does not work with Gmail
//        return readFileUsingReliablePipe()
    }

    // This solution works with gmail.
    private fun readFileUsingOpenDocument(documentId: String?, mode: String?): ParcelFileDescriptor {
        val file = File(context?.cacheDir, "cachefile.txt").also {
            FileOutputStream(it).use { output ->
                context?.resources?.openRawResource(R.raw.dynamodb)?.readBytes()?.let { bytes ->
                    output.write(bytes, 0, bytes.size)
                    output.flush()
                }
            }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.parseMode(mode))
    }

    //        this solution (with createReliablePipe) does not work with gmail, message from gmail: "Can't attach empty file"
//        This solution (createReliablePipe) does work with the ACTION_OPEN_DOCUMENT intent launched from the MainActivity
    //        When not using an asynctask, the app hangs when large files are processed.
    // todo how to provide feedback of the download status?
    private fun readFileUsingReliablePipe(): ParcelFileDescriptor {
        val pipes = ParcelFileDescriptor.createReliablePipe()
        context?.resources?.openRawResource(R.raw.dynamodb)?.readBytes()?.let {
            WriteFileTask(pipes[1], it).execute()
        }
        return pipes[0]
    }

    override fun queryChildDocuments(parentDocumentId: String?, projection: Array<out String>?, sortOrder: String?): Cursor {
        Timber.i("queryChildDocuments(parentDocumentId, projection, sortOrder)($parentDocumentId, $projection, $sortOrder)")

        val notifyUri = DocumentsContract.buildChildDocumentsUri(BuildConfig.DOCUMENTS_AUTHORITY, parentDocumentId)
        val documents = cache.get(notifyUri)
        val cursor = if (documents == null) {
            Timber.i("Documents not in cache, fetching from network.")
            createLoadingCursor(projection).apply {
                setNotificationUri(context?.contentResolver, notifyUri)
            }.also {
                loadFromNetwork(parentDocumentId, notifyUri)
            }
        } else {
            // Documents are found in cache, return the documents by adding them to the cursor.
            Timber.i("Documents in cache, returning.")
            MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).also { cursor ->
                documents.forEach {
                    it.addToRow(cursor.newRow())
                }
            }
        }

        return cursor
    }

    private fun createLoadingCursor(projection: Array<out String>?): MatrixCursor {
        return object : MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION) {
            override fun getExtras(): Bundle {
                return Bundle().apply {
                    putBoolean(DocumentsContract.EXTRA_LOADING, true)
                }

            }
        }
    }

    /**
     * Loads the documents from the network and puts them in the cache. When the network call is finished, the notifyChanged method is
     * called on the contentResolver. This does not deliver the results but signals the SAF framework to call queryChildDocuments again. This
     * time he documents can be returned from the cache.
     */
    private fun loadFromNetwork(parentDocumentId: String?, notifyUri: Uri) {
        GlobalScope.launch {
            delay(1500)
            if (parentDocumentId == ROOT_FOLDER_ID) {
                /**
                 * If the parentDocumentId is the root, we return the files and folders directly below the root (the first level).
                 */
                val documents = mutableListOf<DocumentMetaData>()
                for (i in 0..10) {
                    documents.add(
                        DocumentMetaData(
                            i.toString(),
                            if (i == 0) DocumentsContract.Document.MIME_TYPE_DIR else "text/plain",
                            "name$i",
                            0,
                            0, 10
                        )
                    )
                }
                cache.add(notifyUri, documents)
            } else {
                /**
                 * Return the files and folders below the selected parentDocumentId.
                 */
                val documents = mutableListOf<DocumentMetaData>()
                for (i in 0..2) {
                    documents.add(
                        DocumentMetaData(
                            i.toString(),
                            "text/plain",
                            "name$i",
                            0,
                            0, 10
                        )
                    )
                }
                cache.add(notifyUri, documents)
            }

            Timber.i("openChildDocuments loaded, notifyChange $notifyUri")
            context?.contentResolver?.notifyChange(notifyUri, null, false)
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        Timber.i("queryDocument(documentId, projection)($documentId,$projection)")
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        /**
         * When the root folder is opened, we return a cursor containing the root. Mention that the mime type of the root is a
         * directory because there are typical more files and folders below the root folder. This indicaties to SAF the for this
         * document, the queryChildDocuments should be queried.
         */
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
        } else {
//            non-root document is queried. Provide the correct display name and mime type (for openDocument).
            val row = cursor.newRow()
            with(row) {
                add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, documentId)
                add(DocumentsContract.Document.COLUMN_MIME_TYPE, "application/pdf")
                add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, "sample.pdf")
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

    /**
     * This is the root of the documents. For example when in Gmail an attachment is added, this root is visible in the Open from dialog.
     */
    override fun queryRoots(projection: Array<out String>?): Cursor {
        Timber.i("queryRoots")
        return MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
            .apply {
                val row = newRow()
                with(row) {
                    add(DocumentsContract.Root.COLUMN_ROOT_ID, ROOT_ID)
                    add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_launcher_foreground)
                    add(DocumentsContract.Root.COLUMN_TITLE, context?.getString(R.string.app_name) ?: "DocumentsProviderExample")
                    add(DocumentsContract.Root.COLUMN_FLAGS, 0)
                    add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ROOT_FOLDER_ID)
                }
            }
    }
}