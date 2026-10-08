package com.joshrpr.knifetracker

import android.app.Application
import com.joshrpr.knifetracker.data.KnifeDatabase
import com.joshrpr.knifetracker.data.PhotoStorage

class KnifeTrackerApp : Application() {
    val database: KnifeDatabase by lazy { KnifeDatabase.create(this) }
    val photos: PhotoStorage by lazy { PhotoStorage(this) }
}
