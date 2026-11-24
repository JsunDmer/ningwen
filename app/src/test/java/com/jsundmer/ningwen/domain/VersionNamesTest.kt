package com.jsundmer.ningwen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionNamesTest {

  @Test fun compare_orders_semver_segments() {
    assertTrue(VersionNames.compare("1.0.3", "1.0.2") > 0)
    assertTrue(VersionNames.compare("1.0.2", "1.0.3") < 0)
    assertEquals(0, VersionNames.compare("1.0.3", "1.0.3"))
    assertTrue(VersionNames.compare("1.1.0", "1.0.9") > 0)
    assertTrue(VersionNames.compare("2.0.0", "1.9.9") > 0)
  }

  @Test fun strip_leading_v_from_tag() {
    assertEquals("1.0.3", VersionNames.normalize("v1.0.3"))
    assertEquals("1.0.3", VersionNames.normalize("V1.0.3"))
    assertEquals("1.0.3", VersionNames.normalize("1.0.3"))
  }

  @Test fun compare_handles_uneven_segment_counts() {
    assertTrue(VersionNames.compare("1.0.1", "1.0") > 0)
    assertTrue(VersionNames.compare("1.0", "1.0.0") == 0)
  }
}
