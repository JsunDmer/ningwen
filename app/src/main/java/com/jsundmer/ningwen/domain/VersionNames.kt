package com.jsundmer.ningwen.domain

object VersionNames {
  fun normalize(raw: String): String = raw.trim().removePrefix("v").removePrefix("V")

  fun compare(left: String, right: String): Int {
    val a = segments(normalize(left))
    val b = segments(normalize(right))
    val n = maxOf(a.size, b.size)
    for (i in 0 until n) {
      val av = a.getOrElse(i) { 0 }
      val bv = b.getOrElse(i) { 0 }
      if (av != bv) return av.compareTo(bv)
    }
    return 0
  }

  private fun segments(version: String): List<Int> =
    version.split('.').map { it.toIntOrNull() ?: 0 }
}
