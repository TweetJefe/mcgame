package org.chess


import game.moves.MovementRejection
import game.moves.TurnRejection
import game.core.Game
import game.economy.GoldRules
import game.terrain.MudCell
import game.heroes.faun.FaunView
import game.heroes.clownganger.ClownView
import game.heroes.AbilityRequest
import game.heroes.Abilities
import game.draft.DraftCatalog
import org.chess.heroes.HeroPresentationRegistry
import org.chess.heroes.HeroPresentations
import org.chess.heroes.HeroViewContext
import org.chess.heroes.HeroPanelControls
import org.chess.heroes.HeroPanel
import game.moves.AvailableMoves
import game.moves.MoveResult
import game.moves.Promotion
import game.scenarios.TrainingScenario
import game.state.SquareColor
import game.state.GameMode
import game.state.PieceState

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
fun ChessBoard(game: Game, scenario: TrainingScenario? = null, onRestart: () -> Unit = {},
               onSave: (() -> Unit)? = null, onLoad: (() -> Unit)? = null, fileMessage: String? = null,
               onNewGame: () -> Unit = {}, presentations: HeroPresentationRegistry = HeroPresentations.standard) {
    var snapshot by remember(game) { mutableStateOf(game.snapshot) }
    var selectedId by remember(game) { mutableStateOf<Long?>(null) }
    var selectedAbility by remember(game) { mutableStateOf<Abilities?>(null) }
    var selectedBorrowed by remember(game) { mutableStateOf(false) }
    var selectedTarget by remember(game) { mutableStateOf<Position?>(null) }
    var moveError by remember(game) { mutableStateOf<String?>(null) }
    var pendingPromotion by remember(game) { mutableStateOf<Pair<Position, Position>?>(null) }
    var showDraft by remember(game) { mutableStateOf(false) }
    var claimWithMove by remember(game) { mutableStateOf(false) }
    val moba = snapshot.mode() == GameMode.MOBA
    val clock = snapshot.clock()
    val outcome = snapshot.outcome()
    val currentPlayer = if (snapshot.active() == FigureColor.WHITE) game.whitePlayer else game.blackPlayer
    val selected = selectedId?.let { snapshot.pieces()[it] }?.takeIf {
        it.status() == PieceState.Status.BOARD && !it.isPotted && it.color() == snapshot.active()
    }
    val catalogHeroes = remember(game) { DraftCatalog.current(game.heroRules.definitions()).heroes() }
    val heroes = remember(game) { game.heroRegistry }
    remember(game, presentations) { presentations.validate(heroes, game.heroRules.definitions()) }
    val selectedPresentation = selected?.let { presentations[it.hero()] }
    val heroContext = selected?.let { HeroViewContext(snapshot, it, selectedAbility, heroes) }
    val selectedPos = selected?.position()
    val legal = remember(snapshot.version(), selectedId, selectedAbility) {
        if (selectedPos != null && selectedAbility == null) if (FaunView.hasHiddenEnemy(snapshot)) FaunView.moves(snapshot,selectedPos) else game.getLegalMoves(selectedPos)
        else AvailableMoves(emptyList(), emptyList())
    }
    val requiresDestination = selectedAbility?.targeting()?.requiresDestination() == true
    val abilityTargets = remember(snapshot.version(), selectedId, selectedAbility, selectedBorrowed, selectedTarget) {
        if (selectedPos != null && selectedAbility != null) {
            if (requiresDestination && selectedTarget != null) game.getAbilityDestinations(selectedPos, selectedAbility, selectedTarget, selectedBorrowed)
            else game.getAbilityTargets(selectedPos, selectedAbility, selectedBorrowed)
        } else emptyList()
    }

    fun clearSelection() { selectedId = null; selectedAbility = null; selectedBorrowed = false; selectedTarget = null }
    fun updateView(result: MoveResult) {
        moveError = if (result.isSuccess) null else result.message
        snapshot = game.snapshot
    }
    fun submitMove(from: Position, to: Position, promotion: Promotion? = null) {
        val result = if (claimWithMove) game.claimDraw(currentPlayer, from, to, promotion)
            else game.moveFigure(currentPlayer, from, to, promotion)
        if (result == MovementRejection.PROMOTION_REQUIRED) pendingPromotion = from to to
        else {
            pendingPromotion = null
            claimWithMove = false
            selectedAbility = null
            if (!moba || !result.isSuccess) clearSelection()
        }
        updateView(result)
    }
    fun chooseAbility(ability: Abilities, borrowed: Boolean = false) {
        if (!ability.targeting().requiresTarget()) {
            val request = AbilityRequest(selectedPos, ability, null, null, borrowed)
            if (!borrowed && selectedPresentation?.retainSelection(ability) == true) {
                selectedAbility = null; selectedBorrowed = false; selectedTarget = null
            } else clearSelection()
            updateView(game.useAbility(currentPlayer, request))
            return
        }
        val same = selectedAbility == ability && selectedBorrowed == borrowed
        selectedAbility = if (same) null else ability
        selectedBorrowed = borrowed; selectedTarget = null
        moveError = if (selectedAbility != null && game.getAbilityTargets(selectedPos, ability, borrowed).isEmpty())
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
    fun goldLabel(color: FigureColor) = if (snapshot.economy().enabled()) " • Золото: ${GoldRules.format(snapshot.economy().balanceHalves(color))}" else ""
    fun sideName(color: FigureColor) = if (color == FigureColor.WHITE) "Белые" else "Чёрные"

    Row(Modifier.fillMaxSize().padding(16.dp)) {
        Column {
            Text("Чёрные: ${if (clock.isEnabled) formatTime(clock.blackMillis) else "без часов"}${goldLabel(FigureColor.BLACK)}",
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
                            val actualPiece = snapshot.pieceAt(square)
                            val visible = FaunView.visible(snapshot, actualPiece)
                            val figure = if (visible) snapshot.field().getFigureByPosition(square) else null
                            val piece = if (visible) ClownView.displayed(snapshot,actualPiece) else null
                            val own = piece?.color() == snapshot.active()
                            val isMove = legal.canMove.contains(square)
                            val isCapture = legal.canAttack.contains(square)
                            val isAbilityTarget = abilityTargets.contains(square)
                            CellView(column, row, figure, square == selectedPos, isMove, isCapture,
                                heroLabel = piece?.let { presentations[it.hero()].boardBadge(HeroViewContext(snapshot, it, null, heroes)) },
                                heroDecoration = { piece?.let { presentations[it.hero()].CellDecoration(HeroViewContext(snapshot, it, null, heroes)) } },
                                lightningLabel = if (visible && actualPiece != null) snapshot.lightning().charges().entries
                                    .filter { it.key.targetId() == actualPiece.id() }.sortedBy { it.key.casterId() }
                                    .joinToString(" ") { (snapshot.pieces()[it.key.casterId()]?.let { caster -> presentations[caster.hero()].badge } ?: "") + it.value.count() }.ifEmpty { null } else null,
                                isPotted = actualPiece?.isPotted == true,
                                isBlinded = piece?.isBlinded == true,
                                isStunned = piece?.isStunned == true,
                                isBoxed = piece?.isBoxed == true,
                                isMine = snapshot.mines().cells()[square]?.let { snapshot.pieces()[it.ownerId()]?.color() == snapshot.active() } == true,
                                isInvisible = piece?.let { snapshot.faun().invisible().containsKey(it.id()) } == true,
                                isCloud = snapshot.faun().silenced(square),
                                isMud = snapshot.terrain().cells().containsKey(square),
                                isAmbush = snapshot.terrain().cells()[square]?.kind() == MudCell.Kind.AMBUSH,
                                isBountyMarked = piece?.let { snapshot.economy().mark(it.id()) != null } == true,
                                isAbilityTarget = isAbilityTarget,
                                onClick = {
                                    if (outcome.isFinished || pendingPromotion != null) return@CellView
                                    if (selectedAbility != null && isAbilityTarget && selectedPos != null) {
                                        if (requiresDestination && selectedTarget == null) {
                                            selectedTarget = square; moveError = null
                                        } else {
                                            val request = AbilityRequest(selectedPos, selectedAbility,
                                                if (requiresDestination) selectedTarget else square,
                                                if (requiresDestination) square else null, selectedBorrowed)
                                            val result = game.useAbility(currentPlayer, request)
                                            selectedAbility = null; selectedBorrowed = false; selectedTarget = null
                                            updateView(result)
                                        }
                                    } else if (selectedAbility == null && selectedPos != null && (isMove || isCapture)) {
                                        submitMove(selectedPos, square)
                                    } else if (square == selectedPos) clearSelection()
                                    else if (selectedAbility == null && selectedPos != null && !own && FaunView.hasHiddenEnemy(snapshot)) {
                                        submitMove(selectedPos,square) // Blind capture attempts use the real board.
                                    } else if (own) {
                                        selectedId = piece?.id(); selectedAbility = null; selectedBorrowed = false; selectedTarget = null; moveError = null
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
            Text("Белые: ${if (clock.isEnabled) formatTime(clock.whiteMillis) else "без часов"}${goldLabel(FigureColor.WHITE)}",
                Modifier.padding(start = 60.dp, top = 8.dp), fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = if (!outcome.isFinished && snapshot.active() == FigureColor.WHITE) Color(0xFF2E7D32) else Color.Black)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(if (moba) "Moba Chess" else "Обычные шахматы", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            snapshot.draft()?.let { Button(onClick = { showDraft = true }) { Text("Пики и баны") } }
            scenario?.let {
                Text(it.getTitle(), fontWeight = FontWeight.Bold, color = Color(0xFF6A5ACD))
                Text(it.getInstructions(), fontSize = 13.sp)
                Text("Без часов • ${it.getSettingsLabel()}",
                    fontSize = 12.sp, color = Color.Gray)
                Row {
                    Button(onClick = onRestart) { Text("Заново") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onNewGame) { Text("К списку") }
                }
            }
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
                    Text(selectedPresentation!!.title(heroContext!!), fontWeight = FontWeight.Bold)
                    if (selected.isBlinded) Text("Ослепление: ${selected.blindTurns()} ход владельца. Взятия недоступны.", fontSize = 12.sp, color = Color(0xFFC65D00))
                    if (selectedPresentation.beforeInventory) HeroPanel(selectedPresentation, heroContext,
                        HeroPanelControls(game, selectedPos!!) { ability, borrowed -> chooseAbility(ability, borrowed) })
                    Row {
                        selected.inventory().forEachIndexed { slot, item ->
                            Button(enabled = !item.isEmpty, onClick = { updateView(game.useItem(currentPlayer, selected.id(), slot)) },
                                modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                Text("${slot + 1}: ${item.itemId() ?: "пусто"}", fontSize = 12.sp)
                            }
                        }
                    }
                } else if (moba) Text("Выберите свою фигуру. " + catalogHeroes
                    .joinToString { type -> "${presentations[type].shortName} — ${presentations[type].badge}" } + ".", fontSize = 13.sp, color = Color.Gray)
            }
            val hidden = snapshot.pieces().values.filter { it.status() == PieceState.Status.HIDDEN_BOG }
            if (hidden.isNotEmpty()) Text("В болоте: " + hidden.joinToString {
                (if (it.color() == FigureColor.WHITE) "белый " else "чёрный ") + presentations[it.hero()].shortName
            } + ". Выход только при срабатывании.", fontSize = 12.sp, color = Color(0xFF275D38))
            if (moba && heroContext != null && selectedPresentation != null && !selectedPresentation.beforeInventory)
                HeroPanel(selectedPresentation, heroContext,
                    HeroPanelControls(game, selectedPos!!) { ability, borrowed -> chooseAbility(ability, borrowed) })
            if (moba && selected?.isBoxed == true) Text("В коробке: защищена и не действует до конца следующего собственного хода", fontSize = 13.sp)
            if (moba && selected?.isStunned == true) Text("Оглушён до конца следующего хода соперника источника", fontSize = 12.sp, color = Color(0xFF8A4F00))
            if (requiresDestination) {
                Text(if (selectedTarget == null) "Выберите подсвеченную цель" else "Выберите подсвеченную клетку назначения", fontSize = 13.sp)
                if (selectedTarget != null) Button(onClick = { selectedTarget = null }) { Text("Выбрать другую цель") }
            }
            moveError?.let { Text(it, color = Color.Red) }
            Spacer(Modifier.height(8.dp))
            val whiteHistory = if (moba) snapshot.events().filter { it.color() == FigureColor.WHITE }.map { FaunView.message(snapshot,it) }
                else snapshot.moves().filter { it.color == FigureColor.WHITE }.map { it.toString() }
            val blackHistory = if (moba) snapshot.events().filter { it.color() == FigureColor.BLACK }.map { FaunView.message(snapshot,it) }
                else snapshot.moves().filter { it.color == FigureColor.BLACK }.map { it.toString() }
            Row(Modifier.weight(1f)) {
                MoveHistoryColumn("Белые", whiteHistory)
                MoveHistoryColumn("Чёрные", blackHistory)
            }
            if (onSave != null && onLoad != null) {
                Row {
                    Button(enabled = pendingPromotion == null, onClick = onSave) { Text("Сохранить") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onLoad) { Text("Загрузить") }
                }
                fileMessage?.let { Text(it, fontSize = 12.sp) }
            }
        }
    }
    if (showDraft) snapshot.draft()?.let { initial ->
        AlertDialog(onDismissRequest = { showDraft = false }, title = { Text("Исходные пики и баны") },
            text = { Column {
                Text("Баны: " + initial.bans().joinToString { it.getName() })
                for (side in listOf(FigureColor.WHITE, FigureColor.BLACK)) {
                    Text(if (side == FigureColor.WHITE) "Белые" else "Чёрные", fontWeight = FontWeight.Bold)
                    Text(initial.picksFor(side).joinToString { "${it.hero().getName()} — ${'a' + it.square().column}${it.square().row + 1}" }.ifEmpty { "Обычные фигуры" })
                }
            } }, confirmButton = { Button(onClick = { showDraft = false }) { Text("Закрыть") } })
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
