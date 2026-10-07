package org.chess

import androidx.compose.foundation.layout.*
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import game.*
import kotlinx.coroutines.delay
import model.FigureColor
import model.Position

@Composable
fun CoordinateText(text: String) {
    Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun ChessBoard(game: Game, onNewGame: () -> Unit = {}) {
    var snapshot by remember(game) { mutableStateOf(game.snapshot) }
    var selectedId by remember(game) { mutableStateOf<Long?>(null) }
    var selectedAbility by remember(game) { mutableStateOf<Ability?>(null) }
    var moveError by remember(game) { mutableStateOf<String?>(null) }
    var pendingPromotion by remember(game) { mutableStateOf<Pair<Position, Position>?>(null) }
    var claimWithMove by remember(game) { mutableStateOf(false) }
    val moba = snapshot.mode() == GameMode.MOBA
    val clock = snapshot.clock()
    val outcome = snapshot.outcome()
    val currentPlayer = if (snapshot.active() == FigureColor.WHITE) game.whitePlayer else game.blackPlayer
    val selected = selectedId?.let { snapshot.pieces()[it] }?.takeIf {
        it.status() == PieceState.Status.BOARD && it.color() == snapshot.active()
    }
    val selectedPos = selected?.position()
    val legal = remember(snapshot.version(), selectedId, selectedAbility) {
        if (selectedPos != null && selectedAbility == null) game.getLegalMoves(selectedPos)
        else AvailableMoves(emptyList(), emptyList())
    }
    val abilityTargets = remember(snapshot.version(), selectedId, selectedAbility) {
        if (selectedPos != null && selectedAbility != null) game.getAbilityTargets(selectedPos, selectedAbility) else emptyList()
    }

    fun clearSelection() { selectedId = null; selectedAbility = null }
    fun updateView(result: MoveResult) {
        moveError = if (result.isSuccess) null else result.message
        snapshot = game.snapshot
    }
    fun submitMove(from: Position, to: Position, promotion: Promotion? = null) {
        val result = if (claimWithMove) game.claimDraw(currentPlayer, from, to, promotion)
            else game.moveFigure(currentPlayer, from, to, promotion)
        if (result == MoveResult.PROMOTION_REQUIRED) pendingPromotion = from to to
        else {
            pendingPromotion = null
            claimWithMove = false
            selectedAbility = null
            if (!moba || !result.isSuccess) clearSelection()
        }
        updateView(result)
    }
    fun chooseAbility(ability: Ability) {
        selectedAbility = if (selectedAbility == ability) null else ability
        moveError = if (selectedAbility != null && game.getAbilityTargets(selectedPos, ability).isEmpty())
            "Нет допустимых целей. Выберите другую фигуру или завершите ход." else null
    }

    // Every command source is reflected by a coherent snapshot, including external API calls.
    LaunchedEffect(game) {
        while (true) {
            snapshot = game.snapshot
            if (snapshot.outcome().isFinished) {
                pendingPromotion = null; claimWithMove = false; clearSelection()
                break
            }
            delay(100L)
        }
    }
    LaunchedEffect(snapshot.turn().number()) {
        clearSelection(); claimWithMove = false; pendingPromotion = null
    }
    fun formatTime(millis: Long): String {
        val seconds = (millis + 999L) / 1000L
        return "%02d:%02d".format(seconds / 60L, seconds % 60L)
    }
    fun sideName(color: FigureColor) = if (color == FigureColor.WHITE) "Белые" else "Чёрные"

    Row(Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text("Чёрные: ${if (clock.isEnabled) formatTime(clock.blackMillis) else "без часов"}",
                Modifier.padding(start = 60.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = if (!outcome.isFinished && snapshot.active() == FigureColor.BLACK) Color.Red else Color.Black)
            Text(if (outcome.isFinished) "Партия завершена" else
                "Ход: ${sideName(snapshot.active())}${if (game.isInCheck) " — шах" else ""}",
                Modifier.padding(start = 60.dp, bottom = 8.dp), color = Color.Gray)
            Row {
                Spacer(Modifier.size(60.dp))
                ('a'..'h').forEach { CoordinateText(it.toString()) }
                Spacer(Modifier.size(60.dp))
            }
            Row {
                Column { for (row in 7 downTo 0) CoordinateText((row + 1).toString()) }
                Column {
                    for (row in 7 downTo 0) Row {
                        for (column in 0..7) {
                            val square = Position(row, column)
                            val figure = snapshot.field().getFigureByPosition(square)
                            val piece = snapshot.pieceAt(square)
                            val own = piece?.color() == snapshot.active()
                            val isMove = legal.canMove.contains(square)
                            val isCapture = legal.canAttack.contains(square)
                            val isAbilityTarget = abilityTargets.contains(square)
                            CellView(column, row, figure, square == selectedPos, isMove, isCapture,
                                heroLabel = if (piece?.hero() == HeroType.PUDGE_ROOK) if (piece.storedPieceId() != null) "П●" else "П" else null,
                                isAbilityTarget = isAbilityTarget,
                                onClick = {
                                    if (outcome.isFinished || pendingPromotion != null) return@CellView
                                    if (selectedAbility != null && isAbilityTarget && selectedPos != null) {
                                        val result = game.useAbility(currentPlayer, selectedPos, square, selectedAbility)
                                        selectedAbility = null
                                        updateView(result)
                                    } else if (selectedAbility == null && selectedPos != null && (isMove || isCapture)) {
                                        submitMove(selectedPos, square)
                                    } else if (square == selectedPos) clearSelection()
                                    else if (own) {
                                        selectedId = piece?.id(); selectedAbility = null; moveError = null
                                    } else clearSelection()
                                })
                        }
                    }
                }
                Column { for (row in 7 downTo 0) CoordinateText((row + 1).toString()) }
            }
            Row {
                Spacer(Modifier.size(60.dp))
                ('a'..'h').forEach { CoordinateText(it.toString()) }
                Spacer(Modifier.size(60.dp))
            }
            Text("Белые: ${if (clock.isEnabled) formatTime(clock.whiteMillis) else "без часов"}",
                Modifier.padding(start = 60.dp, top = 8.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = if (!outcome.isFinished && snapshot.active() == FigureColor.WHITE) Color(0xFF2E7D32) else Color.Black)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(if (moba) "Moba Chess • Пудж-Ладья" else "Обычные шахматы", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (outcome.isFinished) {
                Text(outcome.message, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Button(onClick = onNewGame) { Text("Новая партия") }
            } else {
                if (moba) {
                    Text("Перемещение: ${if (snapshot.turn().moveUsed()) "выполнено" else "обязательно"}")
                    Text("Скилл / ульта: ${if (snapshot.turn().abilityUsed()) "использован" else "необязателен"}")
                    Button(enabled = snapshot.turn().canEndTurn() && pendingPromotion == null,
                        onClick = { clearSelection(); updateView(game.endTurn(currentPlayer)) }) { Text("Закончить ход") }
                    if (!snapshot.turn().moveUsed()) Text("Сначала сделайте ход фигурой", color = Color.Gray, fontSize = 13.sp)
                }
                Row {
                    Button(onClick = { clearSelection(); updateView(game.resign(currentPlayer)) }) { Text("Сдаться") }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = snapshot.drawOffer() == FigureColor.NONE && snapshot.moves().any { it.color == FigureColor.WHITE }
                        && snapshot.moves().any { it.color == FigureColor.BLACK },
                        onClick = { updateView(game.offerDraw(currentPlayer)) }) { Text("Ничья") }
                }
                if (!moba) {
                    Button(enabled = pendingPromotion == null && (game.canClaimDraw(currentPlayer) || selected != null), onClick = {
                        if (game.canClaimDraw(currentPlayer)) { clearSelection(); updateView(game.claimDraw(currentPlayer)) }
                        else { claimWithMove = true; moveError = null }
                    }) { Text("Заявить ничью") }
                    if (claimWithMove) Text("Выберите цель объявленного хода. Он будет проверен, но не выполнен.")
                }
                if (snapshot.drawOffer() != FigureColor.NONE) {
                    val recipient = if (snapshot.drawOffer() == FigureColor.WHITE) game.blackPlayer else game.whitePlayer
                    Text("${sideName(snapshot.drawOffer())} предлагают ничью")
                    Row {
                        Button(onClick = { clearSelection(); updateView(game.acceptDraw(recipient)) }) { Text("Принять") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { updateView(game.declineDraw(recipient)) }) { Text("Отказаться") }
                    }
                }
                if (moba && selected != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(if (selected.hero() == HeroType.NONE) selected.type().simpleName else selected.hero().getName(),
                        fontWeight = FontWeight.Bold)
                    if (selected.hero() == HeroType.PUDGE_ROOK) {
                        val hookReady = game.getAbilityAvailability(selectedPos, Ability.HOOK)
                        val ultReady = game.getAbilityAvailability(selectedPos, Ability.SWALLOW)
                        Text("Хук: ${if (selected.cooldown() == 0) "готов" else "КД ${selected.cooldown()}"} • радиус 3", fontSize = 13.sp)
                        Text("Ульта: ${selected.ultimateCharge()} / ${snapshot.pudgeRules().ultimateCaptures()} • радиус 1", fontSize = 13.sp)
                        selected.storedPieceId()?.let { passenger ->
                            Text("Внутри: ${snapshot.pieces()[passenger]?.type()?.simpleName}", color = Color(0xFF6A5ACD))
                        }
                        Row {
                            Button(enabled = hookReady.isSuccess, onClick = { chooseAbility(Ability.HOOK) }) { Text("Хук") }
                            Spacer(Modifier.width(8.dp))
                            Button(enabled = ultReady.isSuccess, onClick = { chooseAbility(Ability.SWALLOW) }) { Text("Проглотить") }
                        }
                        if (selectedAbility != null) Text("Выберите подсвеченную цель", color = Color(0xFF6A5ACD))
                        if (!hookReady.isSuccess && hookReady != MoveResult.ABILITY_ALREADY_USED) Text(hookReady.message, fontSize = 12.sp, color = Color.Gray)
                        if (!ultReady.isSuccess && ultReady != MoveResult.ABILITY_ALREADY_USED) Text(ultReady.message, fontSize = 12.sp, color = Color.Gray)
                    }
                    Row {
                        selected.inventory().forEachIndexed { slot, item ->
                            Button(enabled = !item.isEmpty, onClick = { updateView(game.useItem(currentPlayer, selected.id(), slot)) },
                                modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                Text("${slot + 1}: ${item.itemId() ?: "пусто"}", fontSize = 12.sp)
                            }
                        }
                    }
                } else if (moba) Text("Выберите свою фигуру. Пуджи отмечены буквой П.", fontSize = 13.sp, color = Color.Gray)
            }
            moveError?.let { Text(it, color = Color.Red) }
            Spacer(Modifier.height(8.dp))
            val whiteHistory = if (moba) snapshot.events().filter { it.color() == FigureColor.WHITE }.map { it.message() }
                else snapshot.moves().filter { it.color == FigureColor.WHITE }.map { it.toString() }
            val blackHistory = if (moba) snapshot.events().filter { it.color() == FigureColor.BLACK }.map { it.message() }
                else snapshot.moves().filter { it.color == FigureColor.BLACK }.map { it.toString() }
            Row(Modifier.weight(1f)) {
                MoveHistoryColumn("Белые", whiteHistory)
                MoveHistoryColumn("Чёрные", blackHistory)
            }
        }
    }
    pendingPromotion?.let { pending ->
        AlertDialog(onDismissRequest = { pendingPromotion = null; claimWithMove = false },
            title = { Text("Превращение пешки") },
            text = { Text("Выберите фигуру. Часы продолжают идти.") },
            confirmButton = {
                Column(Modifier.padding(8.dp)) {
                    Promotion.values().forEach { promotion ->
                        val label = when (promotion) {
                            Promotion.QUEEN -> "Ферзь"; Promotion.ROOK -> "Ладья"
                            Promotion.BISHOP -> "Слон"; Promotion.KNIGHT -> "Конь"
                        }
                        Button(onClick = { submitMove(pending.first, pending.second, promotion) }, Modifier.fillMaxWidth()) { Text(label) }
                    }
                }
            },
            dismissButton = { Button(onClick = { pendingPromotion = null; claimWithMove = false }) { Text("Отмена") } })
    }
}
