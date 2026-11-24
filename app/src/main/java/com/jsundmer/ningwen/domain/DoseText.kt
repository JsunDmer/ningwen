package com.jsundmer.ningwen.domain

import java.math.BigDecimal

object DoseText {
  fun normalize(raw: String?): String? = raw?.trim()?.ifEmpty { null }

  /** 旧库里的 REAL 用量。1.0 显示成 1，避免科学计数法。 */
  fun formatLegacy(dose: Double): String =
    BigDecimal(dose.toString()).stripTrailingZeros().toPlainString()
}
