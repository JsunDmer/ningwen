package com.jsundmer.ningwen.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.jsundmer.ningwen.domain.DoseText

@Database(entities = [Record::class], version = 2, exportSchema = false)
abstract class NingwenDatabase : RoomDatabase() {
  abstract fun recordDao(): RecordDao

  companion object {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        var preservedSeq = 0L
        db.query("SELECT seq FROM sqlite_sequence WHERE name = 'records'").use { cursor ->
          if (cursor.moveToFirst() && !cursor.isNull(0)) preservedSeq = cursor.getLong(0)
        }
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `records_new` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `value` REAL NOT NULL,
            `measuredAt` INTEGER NOT NULL,
            `doseTabs` TEXT,
            `note` TEXT
          )
          """.trimIndent(),
        )
        var maxId = 0L
        db.query("SELECT id, value, measuredAt, doseTabs, note FROM records").use { cursor ->
          while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            if (id > maxId) maxId = id
            val dose = if (cursor.isNull(3)) null else DoseText.formatLegacy(cursor.getDouble(3))
            val note = if (cursor.isNull(4)) null else cursor.getString(4)
            db.execSQL(
              "INSERT INTO records_new (id, value, measuredAt, doseTabs, note) VALUES (?, ?, ?, ?, ?)",
              arrayOf(id, cursor.getDouble(1), cursor.getLong(2), dose, note),
            )
          }
        }
        db.execSQL("DROP TABLE records")
        db.execSQL("ALTER TABLE records_new RENAME TO records")
        // sqlite_sequence.name 没有主键，不能 INSERT OR REPLACE，否则会多出一行。
        db.execSQL(
          "UPDATE sqlite_sequence SET seq = ? WHERE name = 'records'",
          arrayOf(maxOf(preservedSeq, maxId)),
        )
      }
    }

    @Volatile private var instance: NingwenDatabase? = null

    fun get(context: Context): NingwenDatabase = instance ?: synchronized(this) {
      instance ?: Room.databaseBuilder(
        context.applicationContext,
        NingwenDatabase::class.java,
        "ningwen.db",
      ).addMigrations(MIGRATION_1_2).build().also { instance = it }
    }
  }
}
