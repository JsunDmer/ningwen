package com.jsundmer.ningwen.ui.diet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsundmer.ningwen.domain.DietCatalog
import com.jsundmer.ningwen.domain.DietGroup
import com.jsundmer.ningwen.domain.InrDietEffect
import com.jsundmer.ningwen.ui.theme.Bad
import com.jsundmer.ningwen.ui.theme.Warn

@Composable
fun DietScreen() {
  LazyColumn(
    modifier = Modifier.fillMaxWidth(),
    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
          DietCatalog.DISCLAIMER,
          modifier = Modifier.padding(16.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 13.sp,
        )
      }
    }
    DietCatalog.groups.forEach { group ->
      item { GroupHeader(group) }
      items(group.items, key = { it.name }) { item ->
        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
          Column(Modifier.padding(14.dp)) {
            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(item.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun GroupHeader(group: DietGroup) {
  Column {
    Text(group.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = effectColor(group.effect))
    Text(group.summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
  }
}

private fun effectColor(effect: InrDietEffect): Color = when (effect) {
  InrDietEffect.LOWER -> Bad
  InrDietEffect.RAISE -> Warn
}
