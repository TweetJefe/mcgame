package org.chess

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    isAbilityTarget: Boolean = false,
    isBlinded: Boolean = false,
    isBountyMarked: Boolean = false,
    isMud: Boolean = false,
    isAmbush: Boolean = false,
    isInvisible: Boolean = false,
    isCloud: Boolean = false,
    isMine: Boolean = false,
    isBoxed: Boolean = false,
    isStunned: Boolean = false,
    lightningLabel: String? = null,
    isPotted: Boolean = false
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
        if (isMud) {
            Box(Modifier.fillMaxSize().background(Color(0xFF275D38).copy(alpha = 0.65f)))
            Text(if (isAmbush) "≈●" else "≈", fontSize = 15.sp, color = Color(0xFFD4E9A6),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 3.dp))
        }
        if (isCloud) {
            Box(Modifier.fillMaxSize().background(Color(0xFF687784).copy(alpha = 0.5f)))
            Text("☁",fontSize=18.sp,color=Color.White,modifier=Modifier.align(Alignment.TopCenter))
        }
        // Рисуем слой подсветки
        Box(Modifier.fillMaxSize().background(overlayColor))

        if (isPotted) {
            Canvas(Modifier.size(42.dp).padding(bottom=5.dp)) {
                drawOval(Color(0xFFB87543),topLeft=Offset(size.width*.12f,size.height*.21f),size=Size(size.width*.76f,size.height*.71f))
                drawRect(Color(0xFFB87543),topLeft=Offset(size.width*.31f,size.height*.09f),size=Size(size.width*.38f,size.height*.24f))
                drawOval(Color(0xFF633820),topLeft=Offset(size.width*.26f,size.height*.04f),size=Size(size.width*.48f,size.height*.17f))
            }
            Text("Горшок",fontSize=10.sp,color=Color(0xFF4A2C16),modifier=Modifier.align(Alignment.BottomCenter))
        } else if (figure != null && figure !is Empty) {
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
            Text(text = symbol, fontSize = 40.sp, color = if (isInvisible) Color.Black.copy(alpha=0.4f) else Color.Black)
            if (isInvisible) Text("Невид",fontSize=10.sp,color=Color.White,modifier=Modifier.align(Alignment.TopStart).background(Color(0xFF44577A)))
            lightningLabel?.let { label -> Text(label,color=Color.Black,fontSize=10.sp,modifier=Modifier.align(Alignment.BottomStart).background(Color(0xFF80DEEA)).padding(horizontal=2.dp)) }
            heroLabel?.let { label ->
                Text(label, color = Color.White, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xFF6A5ACD)).padding(horizontal = 3.dp))
            }
            if (isBountyMarked) Text("+1,5", color = Color.Black, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).background(Color(0xFFFFD54F)).padding(horizontal = 2.dp))
            if (isMine) Text("Мина",color=Color.White,fontSize=10.sp,modifier=Modifier.align(Alignment.BottomStart).background(Color(0xFF444444)).padding(horizontal=2.dp))
            if (isBoxed) Text("Коробка",color=Color.White,fontSize=10.sp,modifier=Modifier.align(Alignment.TopCenter).background(Color(0xFF795548)).padding(horizontal=2.dp))
            if (isStunned) Text("Стан",color=Color.Black,fontSize=11.sp,modifier=Modifier.align(Alignment.BottomStart).background(Color(0xFFFFBD59)).padding(horizontal=2.dp))
            if (isBlinded) Text("Осл", color = Color.White, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopStart).background(Color(0xFFC65D00)).padding(horizontal = 2.dp))
        } else if (isMoveTarget) {
            // Если клетка пустая, но туда можно сходить — рисуем маленькую точку
            Box(
                Modifier.size(16.dp).background(Color.Black.copy(alpha = 0.1f), shape = CircleShape)
            )
        }
    }
}