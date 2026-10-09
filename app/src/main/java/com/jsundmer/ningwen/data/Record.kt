package com.jsundmer.ningwen.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class Record(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val value: Double,
  val measuredAt: Long,
  val doseTabs: Double? = null,
  val note: String? = null,
)
