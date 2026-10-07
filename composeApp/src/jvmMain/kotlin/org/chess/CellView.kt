package org.chess

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import model.FigureColor
import model.figures.Empty
import model.figures.Bishop
import model.figures.King
import model.figures.Knight
import model.figures.Pawn
import model.figures.Queen
import model.figures.Rook

@Composable
fun CellView(
    x: Int,
    y: Int,
    figure: Any?,
    isSelected: Boolean,
    isMoveTarget: Boolean,
    isAttackTarget: Boolean,
    onClick: () -> Unit,
    heroLabel: String? = null,
    isAbilityTarget: Boolean = false
) {
    val isDark = (x + y) % 2 == 0
    val baseColor = if (isDark) Color(0xFF769656) else Color(0xFFEEEED2)

    // Вычисляем цвет подсветки поверх основной клетки
    val overlayColor = when {
        isAbilityTarget -> Color(0xFF8A56AC).copy(alpha = 0.45f)
        isSelected -> Color.Yellow.copy(alpha = 0.4f)
        isAttackTarget -> Color.Red.copy(alpha = 0.4f)
        isMoveTarget -> Color.Green.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(60.dp)
            .background(baseColor)
            .border(0.5.dp, Color.Black.copy(alpha = 0.2f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Рисуем слой подсветки
        Box(Modifier.fillMaxSize().background(overlayColor))

        if (figure != null && figure !is Empty) {
            // Твоя логика отрисовки фигур
            val symbol = when (figure) {
                is Pawn -> if (figure.color == FigureColor.WHITE) "♙" else "♟"
                is Knight -> if (figure.color == FigureColor.WHITE) "♘" else "♞"
                is Queen -> if (figure.color == FigureColor.WHITE) "♕" else "♛"
                is Rook -> if (figure.color == FigureColor.WHITE) "♖" else "♜"
                is Bishop -> if (figure.color == FigureColor.WHITE) "♗" else "♝"
                is King -> if (figure.color == FigureColor.WHITE) "♔" else "♚"
                else -> ""
            }
            Text(text = symbol, fontSize = 40.sp)
            heroLabel?.let { label ->
                Text(label, color = Color.White, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xFF6A5ACD)).padding(horizontal = 3.dp))
            }
        } else if (isMoveTarget) {
            // Если клетка пустая, но туда можно сходить — рисуем маленькую точку
            Box(
                Modifier.size(16.dp).background(Color.Black.copy(alpha = 0.1f), shape = CircleShape)
            )
        }
    }
}