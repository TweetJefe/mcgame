package org.chess

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoveHistoryColumn(title: String, moves: List<String>) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .padding(8.dp)
            .border(1.dp, Color.LightGray)
            .padding(8.dp)
    ) {
        Text(title, fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn {
            itemsIndexed(moves) { index, move ->
                Text("${index + 1}. $move", fontSize = 14.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}