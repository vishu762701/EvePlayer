package com.example

import android.app.Application
import com.example.data.local.EveDatabase
import com.example.data.preferences.AppPreferencesRepository

class EveApplication : Application() {

    val database: EveDatabase by lazy {
        EveDatabase.getDatabase(this)
    }

    val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: EveApplication
            private set
    }
}
