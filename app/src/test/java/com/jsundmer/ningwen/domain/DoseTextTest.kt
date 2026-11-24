package com.jsundmer.ningwen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DoseTextTest {

  @Test fun normalize_keeps_alternating_dose_text() {
    assertEquals("1.25/1.5", DoseText.normalize("  1.25/1.5  "))
    assertEquals("1.5", DoseText.normalize("1.5"))
  }

  @Test fun normalize_blank_to_null() {
    assertNull(DoseText.normalize("   "))
    assertNull(DoseText.normalize(""))
    assertNull(DoseText.normalize(null))
  }

  @Test fun formatLegacy_uses_plain_shortest_decimal() {
    assertEquals("1.25", DoseText.formatLegacy(1.25))
    assertEquals("1.5", DoseText.formatLegacy(1.5))
    assertEquals("1", DoseText.formatLegacy(1.0))
    assertEquals("0.25", DoseText.formatLegacy(0.25))
    assertEquals("2", DoseText.formatLegacy(2.0))
  }
}
