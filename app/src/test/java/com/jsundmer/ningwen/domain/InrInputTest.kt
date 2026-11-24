package com.jsundmer.ningwen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InrInputTest {

  @Test fun parse_accepts_single_number_inside_range() {
    assertEquals(2.4, InrInput.parse("2.4")!!, 0.0)
    assertEquals(2.5, InrInput.parse(" 2.5 ")!!, 0.0)
    assertEquals(0.5, InrInput.parse("0.5")!!, 0.0)
    assertEquals(5.0, InrInput.parse("5.0")!!, 0.0)
  }

  @Test fun parse_rejects_slash_text_and_out_of_range() {
    assertNull(InrInput.parse("1.25/1.5"))
    assertNull(InrInput.parse("0.4"))
    assertNull(InrInput.parse("5.1"))
    assertNull(InrInput.parse("abc"))
    assertNull(InrInput.parse(""))
  }

  @Test fun error_copy_is_fixed() {
    assertEquals("请输入 0.5–5.0 之间的数值", InrInput.ERROR)
  }
}
