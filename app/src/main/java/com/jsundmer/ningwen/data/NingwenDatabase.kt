package com.jsundmer.ningwen.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Record::class], version = 1, exportSchema = false)
abstract class NingwenDatabase : RoomDatabase() {
  abstract fun recordDao(): RecordDao

  companion object {
    @Volatile private var instance: NingwenDatabase? = null

    fun get(context: Context): NingwenDatabase = instance ?: synchronized(this) {
      instance ?: Room.databaseBuilder(
        context.applicationContext,
        NingwenDatabase::class.java,
        "ningwen.db",
      ).build().also { instance = it }
    }
  }
}
