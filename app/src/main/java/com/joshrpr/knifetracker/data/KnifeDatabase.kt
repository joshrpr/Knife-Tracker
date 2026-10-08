package com.joshrpr.knifetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Knife::class, Sharpening::class], version = 1)
abstract class KnifeDatabase : RoomDatabase() {
    abstract fun knifeDao(): KnifeDao

    companion object {
        fun create(context: Context): KnifeDatabase =
            Room.databaseBuilder(context, KnifeDatabase::class.java, "knife-tracker.db").build()
    }
}
