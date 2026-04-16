package com.supermanzer.manzertracker

import android.app.Application
import com.supermanzer.manzertracker.data.AppDatabase

class ManzerTrackerApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
