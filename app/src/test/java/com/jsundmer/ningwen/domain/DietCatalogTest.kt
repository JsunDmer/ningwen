package com.jsundmer.ningwen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DietCatalogTest {

  @Test fun groups_are_lower_then_raise() {
    assertEquals(listOf(InrDietEffect.LOWER, InrDietEffect.RAISE), DietCatalog.groups.map { it.effect })
    assertEquals("可能降低 INR", DietCatalog.groups[0].title)
    assertEquals("可能升高 INR", DietCatalog.groups[1].title)
    assertEquals("维生素 K 较多，可能减弱华法林作用", DietCatalog.groups[0].summary)
    assertEquals("可能增强华法林作用，出血风险增加", DietCatalog.groups[1].summary)
  }

  @Test fun item_names_match_the_reference() {
    assertEquals(
      listOf("绿叶菜", "十字花科", "纳豆", "动物肝脏", "大量绿茶"),
      DietCatalog.groups[0].items.map { it.name },
    )
    assertEquals(
      listOf("西柚", "蔓越莓", "芒果", "大量饮酒", "大蒜、生姜、鱼油、银杏"),
      DietCatalog.groups[1].items.map { it.name },
    )
  }

  @Test fun disclaimer_is_the_fixed_notice() {
    assertEquals(
      "以下为常见饮食对华法林使用者 INR 的一般影响，仅供查看，不能代替医生或药师的意见。平时吃得稳定通常问题不大；突然比平时吃得明显更多或更少，影响才比较大。",
      DietCatalog.DISCLAIMER,
    )
  }

  @Test fun details_are_present_and_names_unique() {
    val names = DietCatalog.groups.flatMap { group -> group.items.map { it.name } }
    assertEquals(names.size, names.toSet().size)
    DietCatalog.groups.flatMap { it.items }.forEach { item ->
      assertTrue(item.name.isNotBlank())
      assertTrue(item.detail.isNotBlank())
    }
  }
}
