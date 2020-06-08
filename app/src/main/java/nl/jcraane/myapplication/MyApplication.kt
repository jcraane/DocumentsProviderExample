package nl.jcraane.myapplication

import android.app.Application
import timber.log.Timber

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree(applicationContext))
    }
}