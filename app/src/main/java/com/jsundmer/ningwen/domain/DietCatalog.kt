package com.jsundmer.ningwen.domain

enum class InrDietEffect { LOWER, RAISE }

data class DietItem(val name: String, val detail: String)

data class DietGroup(
  val effect: InrDietEffect,
  val title: String,
  val summary: String,
  val items: List<DietItem>,
)

object DietCatalog {
  const val DISCLAIMER =
    "以下为常见饮食对华法林使用者 INR 的一般影响，仅供查看，不能代替医生或药师的意见。平时吃得稳定通常问题不大；突然比平时吃得明显更多或更少，影响才比较大。"

  val groups: List<DietGroup> = listOf(
    DietGroup(
      effect = InrDietEffect.LOWER,
      title = "可能降低 INR",
      summary = "维生素 K 较多，可能减弱华法林作用",
      items = listOf(
        DietItem("绿叶菜", "菠菜、芥蓝、生菜、油麦菜、羽衣甘蓝。突然比平时吃得多，INR 可能下降。"),
        DietItem("十字花科", "西兰花、卷心菜、抱子甘蓝。"),
        DietItem("纳豆", "维生素 K 很高，常会明显拉低 INR。"),
        DietItem("动物肝脏", "猪肝、鸡肝等。"),
        DietItem("大量绿茶", "喝得比平时多时，INR 可能下降。"),
      ),
    ),
    DietGroup(
      effect = InrDietEffect.RAISE,
      title = "可能升高 INR",
      summary = "可能增强华法林作用，出血风险增加",
      items = listOf(
        DietItem("西柚", "葡萄柚和葡萄柚汁也算，可能让 INR 升高。"),
        DietItem("蔓越莓", "大量果实或果汁。"),
        DietItem("芒果", "有报告称可能升高 INR。"),
        DietItem("大量饮酒", "尤其是比平时喝得更多。"),
        DietItem("大蒜、生姜、鱼油、银杏", "量大时可能增强抗凝作用。"),
      ),
    ),
  )
}
