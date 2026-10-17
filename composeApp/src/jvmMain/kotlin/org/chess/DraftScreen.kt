package org.chess

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import game.draft.*
import game.heroes.HeroType
import kotlinx.coroutines.delay
import model.Field
import model.FigureColor
import model.Position
import model.figures.*

private fun draftSide(side: FigureColor) = if (side == FigureColor.WHITE) "Белые" else "Чёрные"
private fun draftCell(square: Position) = "${'a' + square.column}${square.row + 1}"
private fun heroBadge(hero: HeroType) = when (hero) {
    HeroType.PUDGE_ROOK -> "П"; HeroType.LUMESHARD_BISHOP -> "L"
    HeroType.BOUNTY_CROW_PAWN -> "C"; HeroType.BOLOTNIK_KNIGHT -> "Б"; HeroType.IZBA_ROOK -> "И"; HeroType.GINSENG_BISHOP -> "G"; else -> ""
}

@Composable
fun DraftScreen(draft: Draft, onStart: (DraftSnapshot) -> Unit, onBack: () -> Unit) {
    var snapshot by remember(draft) { mutableStateOf(draft.snapshot) }
    var selectedHero by remember(draft, snapshot.version()) { mutableStateOf<HeroType?>(null) }
    var selectedSquare by remember(draft, snapshot.version(), selectedHero) { mutableStateOf<Position?>(null) }
    var error by remember(draft) { mutableStateOf<String?>(null) }
    val board = remember { Field(8) }
    val placements = snapshot.assignments()
    val targets = if (snapshot.phase() == DraftPhase.PICKS && selectedHero != null)
        draft.getPlacementTargets(snapshot.active(), selectedHero) else emptyList()
    fun update(result: DraftActionResult) {
        error = if (result.isSuccess) null else result.message
        snapshot = draft.snapshot
    }
    LaunchedEffect(draft) {
        while (true) { snapshot = draft.snapshot; delay(100L) }
    }
    Column(Modifier.fillMaxSize().background(Color(0xFFF7F6F2)).padding(24.dp)) {
        Text("Пики и баны", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(when (snapshot.phase()) {
            DraftPhase.BANS -> "Баны — выбирают ${draftSide(snapshot.active()).lowercase()}"
            DraftPhase.PICKS -> "Пики — выбирают ${draftSide(snapshot.active()).lowercase()}"
            DraftPhase.COMPLETE -> "Составы готовы"
        }, fontSize = 20.sp, color = Color(0xFF6A5ACD))
        Text(when (snapshot.phase()) {
            DraftPhase.BANS -> "Выберите героя и нажмите «Забанить». ${snapshot.rules().bansPerSide()} бан на сторону, герои без повторений."
            DraftPhase.PICKS -> "Выберите героя, затем зелёную стартовую клетку и нажмите «Выбрать». Можно закончить пики раньше."
            DraftPhase.COMPLETE -> "Проверьте составы. Игровые часы начнутся после нажатия «Начать партию»."
        }, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.weight(1f)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                for (hero in HeroCatalog.heroes(snapshot.rules().catalogVersion())) {
                    val ban = snapshot.events().firstOrNull { it.kind() == DraftEvent.Kind.BAN && it.hero() == hero }
                    val pick = snapshot.events().firstOrNull { it.kind() == DraftEvent.Kind.PICK && it.hero() == hero }
                    val available = !snapshot.isComplete && ban == null && pick == null
                    Column(Modifier.fillMaxWidth().height(if (snapshot.rules().catalogVersion() >= 3) 84.dp else 102.dp).padding(bottom = 8.dp)
                        .background(if (ban != null) Color(0xFFE9E3E3) else if (pick != null) Color(0xFFE2EAE0) else Color.White)
                        .border(if (selectedHero == hero) 3.dp else 1.dp, if (selectedHero == hero) Color(0xFF6A5ACD) else Color(0xFFCECECE))
                        .clickable(enabled = available) { selectedHero = hero; error = null }
                        .padding(if (snapshot.rules().catalogVersion() >= 3) 8.dp else 12.dp)) {
                        Text(hero.getName(), fontWeight = FontWeight.Bold, fontSize = 17.sp,
                            lineHeight = if (snapshot.rules().catalogVersion() >= 3) 20.sp else 24.sp)
                        Text(HeroCatalog.summary(hero), fontSize = 12.sp,
                            lineHeight = if (snapshot.rules().catalogVersion() >= 3) 16.sp else 24.sp)
                        Text(when {
                            ban != null -> "Забанен: ${draftSide(ban.side())}"
                            pick != null -> "Выбран: ${draftSide(pick.side())} • ${draftCell(pick.square())}"
                            else -> "Доступен"
                        }, fontSize = 12.sp, color = Color.Gray,
                            lineHeight = if (snapshot.rules().catalogVersion() >= 3) 16.sp else 24.sp)
                    }
                }
                if (snapshot.phase() == DraftPhase.PICKS) {
                    Text(if (selectedHero == null) "Сначала выберите героя" else
                        "${selectedHero!!.getName()}: ${selectedSquare?.let(::draftCell) ?: "выберите зелёную клетку"}", fontWeight = FontWeight.Bold)
                }
                Row {
                    if (snapshot.phase() == DraftPhase.BANS) Button(enabled = selectedHero != null,
                        onClick = { update(draft.ban(snapshot.active(), selectedHero)) }) { Text("Забанить героя") }
                    if (snapshot.phase() == DraftPhase.PICKS) {
                        Button(enabled = selectedHero != null && selectedSquare != null,
                            onClick = { update(draft.pick(snapshot.active(), selectedHero, selectedSquare)) }) { Text("Выбрать героя") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { update(draft.finishPicks(snapshot.active())) }) { Text("Закончить пики") }
                    }
                    if (snapshot.isComplete) Button(onClick = { onStart(snapshot) }) { Text("Начать партию") }
                }
                error?.let { Text(it, color = Color.Red) }
                Text("Остальные фигуры остаются обычными. Ульты заряжаются за 2 собственных взятия, включая пешки.", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.width(440.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Стартовая расстановка", fontWeight = FontWeight.Bold)
                Row {
                    Spacer(Modifier.width(20.dp))
                    for (file in 'a'..'h') Box(Modifier.width(36.dp).height(20.dp), contentAlignment = Alignment.Center) { Text(file.toString(), fontSize = 12.sp) }
                }
                for (row in 7 downTo 0) Row {
                    Box(Modifier.width(20.dp).height(36.dp), contentAlignment = Alignment.Center) { Text((row + 1).toString(), fontSize = 12.sp) }
                    for (col in 0..7) {
                        val square = Position(row, col)
                        val piece = board.getFigureByPosition(square)
                        val symbol = when (piece) {
                            is Pawn -> if (piece.color == FigureColor.WHITE) "♙" else "♟"
                            is Knight -> if (piece.color == FigureColor.WHITE) "♘" else "♞"
                            is Bishop -> if (piece.color == FigureColor.WHITE) "♗" else "♝"
                            is Rook -> if (piece.color == FigureColor.WHITE) "♖" else "♜"
                            is Queen -> if (piece.color == FigureColor.WHITE) "♕" else "♛"
                            is King -> if (piece.color == FigureColor.WHITE) "♔" else "♚"
                            else -> ""
                        }
                        Box(Modifier.size(36.dp).background(when {
                            square == selectedSquare -> Color(0xFFFFD54F)
                            square in targets -> Color(0xFF9BD675)
                            (row + col) % 2 == 0 -> Color(0xFF769656)
                            else -> Color(0xFFEEEED2)
                        }).border(0.5.dp, Color.Black.copy(alpha = 0.15f))
                            .clickable(enabled = square in targets) { selectedSquare = square }, contentAlignment = Alignment.Center) {
                            Text(symbol, fontSize = 26.sp)
                            placements[square]?.let { Text(heroBadge(it), fontSize = 10.sp, color = Color.White,
                                modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xFF6A5ACD)).padding(horizontal = 2.dp)) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    for (side in listOf(FigureColor.WHITE, FigureColor.BLACK)) Column(Modifier.weight(1f).padding(4.dp)) {
                        Text(draftSide(side), fontWeight = FontWeight.Bold)
                        if (snapshot.picksFor(side).isEmpty()) Text("Герои не выбраны", fontSize = 12.sp, color = Color.Gray)
                        for (pick in snapshot.picksFor(side)) Text("${pick.hero().getName()} • ${draftCell(pick.square())}", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.fillMaxWidth().background(Color.White).padding(8.dp)) {
                    Text("История драфта", fontWeight = FontWeight.Bold)
                    if (snapshot.events().isEmpty()) Text("После банов выбирайте героев из оставшегося общего пула.", fontSize = 12.sp, color = Color.Gray)
                    for (event in snapshot.events()) Text("${event.sequence()}. ${draftSide(event.side())}: " + when (event.kind()) {
                        DraftEvent.Kind.BAN -> "бан ${event.hero().getName()}"
                        DraftEvent.Kind.PICK -> "${event.hero().getName()} → ${draftCell(event.square())}"
                        DraftEvent.Kind.FINISH_PICKS -> "пики завершены"
                    }, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onBack) { Text("Назад в меню") }
            Spacer(Modifier.width(16.dp))
            Text("Драфт без таймера • партия 10 минут на сторону", fontSize = 13.sp, color = Color.Gray)
        }
    }
}
