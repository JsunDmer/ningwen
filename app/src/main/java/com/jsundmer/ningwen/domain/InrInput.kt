package com.jsundmer.ningwen.domain

object InrInput {
  const val ERROR = "请输入 0.5–5.0 之间的数值"

  private const val MIN = 0.5
  private const val MAX = 5.0

  fun parse(raw: String): Double? {
    val value = raw.trim().toDoubleOrNull() ?: return null
    if (value < MIN || value > MAX) return null
    return value
  }
}
