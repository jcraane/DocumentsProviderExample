package nl.jcraane.myapplication

import android.content.Context
import android.util.Log
import timber.log.Timber

class DebugTree(private val context: Context) : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, message, t)
    }
}