package com.jsundmer.ningwen.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {
  @Query("SELECT * FROM records ORDER BY measuredAt DESC")
  fun observeAll(): Flow<List<Record>>

  @Query("SELECT * FROM records ORDER BY measuredAt DESC LIMIT 1")
  suspend fun latest(): Record?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(record: Record): Long

  @Delete
  suspend fun delete(record: Record)
}
