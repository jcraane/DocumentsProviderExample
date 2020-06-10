package nl.jcraane.myapplication

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.io.FileInputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        openDocument.setOnClickListener {
            openFile()
        }
    }

    fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        startActivityForResult(intent, PICK_FILE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.i("onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                applicationContext.contentResolver.also { contentResolver ->
                    contentResolver.openFileDescriptor(uri, "r")?.let { parcelFileDescriptor ->
                        val descriptor = parcelFileDescriptor.fileDescriptor
                        Timber.i("fileDesciptor = $descriptor")
                        FileInputStream(descriptor).use {
                            val bytes = it.readBytes()
                            Toast.makeText(this, "Received ${bytes.size} bytes", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val PICK_FILE_REQUEST_CODE = 1
    }
}