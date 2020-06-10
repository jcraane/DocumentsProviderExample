package nl.jcraane.myapplication.picker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_mypicker.*
import nl.jcraane.myapplication.R
import java.io.File
import java.io.FileOutputStream

class MyPickerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypicker)
        done.setOnClickListener {
            val file = File(cacheDir, "dynamodb.pdf").also {
                FileOutputStream(it).use { output ->
                    resources?.openRawResource(R.raw.dynamodb)?.readBytes()?.let { bytes ->
                        output.write(bytes, 0, bytes.size)
                        output.flush()
                    }
                }
            }

            val uri = FileProvider.getUriForFile(this, "nl.jcraane.myapplication.provider", file)
            println("uri to send = $uri")
            val result = Intent();
            result.setData(uri)
            result.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }
}