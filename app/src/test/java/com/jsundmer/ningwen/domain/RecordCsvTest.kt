package com.jsundmer.ningwen.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordCsvTest {

  @Test fun header_stays_the_existing_columns() {
    assertEquals("日期,INR,用量(片),备注", RecordCsv.HEADER)
  }

  @Test fun line_keeps_alternating_dose() {
    assertEquals(
      "2026-11-24,2.4,1.25/1.5,饮食变化",
      RecordCsv.line("2026-11-24", 2.4, "1.25/1.5", "饮食变化"),
    )
  }

  @Test fun line_replaces_ascii_commas() {
    assertEquals(
      "2026-11-24,2.4,1.25，1.5,a，b",
      RecordCsv.line("2026-11-24", 2.4, "1.25,1.5", "a,b"),
    )
  }

  @Test fun line_empty_optional_fields() {
    assertEquals("2026-11-24,2.4,,", RecordCsv.line("2026-11-24", 2.4, null, null))
  }
}
