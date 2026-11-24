package com.jsundmer.ningwen.domain

object RecordCsv {
  const val HEADER = "日期,INR,用量(片),备注"

  fun line(date: String, value: Double, dose: String?, note: String?): String {
    val doseCell = dose.orEmpty().replace(",", "，")
    val noteCell = note.orEmpty().replace(",", "，")
    return "$date,$value,$doseCell,$noteCell"
  }
}
