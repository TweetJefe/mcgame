package org.chess

import game.core.Game
import game.economy.GoldRules
import game.terrain.MudCell
import game.heroes.faun.FaunView
import game.heroes.clownganger.ClownView
import game.heroes.AbilityRequest
import game.heroes.Ability
import game.heroes.HeroType
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
               onNewGame: () -> Unit = {}) {
    var snapshot by remember(game) { mutableStateOf(game.snapshot) }
    var selectedId by remember(game) { mutableStateOf<Long?>(null) }
    var selectedAbility by remember(game) { mutableStateOf<Ability?>(null) }
    var selectedBorrowed by remember(game) { mutableStateOf(false) }
    var displacementTargetId by remember(game) { mutableStateOf<Long?>(null) }
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
    val selectedPos = selected?.position()
    val legal = remember(snapshot.version(), selectedId, selectedAbility) {
        if (selectedPos != null && selectedAbility == null) if (FaunView.hasHiddenEnemy(snapshot)) FaunView.moves(snapshot,selectedPos) else game.getLegalMoves(selectedPos)
        else AvailableMoves(emptyList(), emptyList())
    }
    val displaced = displacementTargetId?.let { snapshot.pieces()[it] }?.takeIf { it.position() != null }
    val abilityTargets = remember(snapshot.version(), selectedId, selectedAbility, selectedBorrowed, displacementTargetId) {
        if (selectedPos != null && selectedAbility != null) {
            if (selectedAbility == Ability.DISPLACE && displaced != null) game.getDisplacementDestinations(selectedPos, displaced.position(), selectedBorrowed)
            else game.getAbilityTargets(selectedPos, selectedAbility, selectedBorrowed)
        } else emptyList()
    }

    fun clearSelection() { selectedId = null; selectedAbility = null; selectedBorrowed = false; displacementTargetId = null }
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
    fun chooseAbility(ability: Ability, borrowed: Boolean = false) {
        val same = selectedAbility == ability && selectedBorrowed == borrowed
        selectedAbility = if (same) null else ability
        selectedBorrowed = borrowed; displacementTargetId = null
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
                                heroLabel = when (piece?.hero()) {
                                    HeroType.PUDGE_ROOK -> if (piece.storedPieceId() != null) "П●" else "П"
                                    HeroType.LUMESHARD_BISHOP -> "L"
                                    HeroType.BOUNTY_CROW_PAWN -> "C"
                                    HeroType.KIKIMORA_BISHOP -> "Ки"
                                    HeroType.CHRONOMLIN_PAWN -> "Ch"
                                    HeroType.ZEUS_ROOK -> "Z"
                                    HeroType.MOLEMINER_PAWN -> "M"
                                    HeroType.CLOWNGANGER_KNIGHT -> "Кл"
                                    HeroType.ILLUSION -> "Коп"
                                    HeroType.FAUN_KNIGHT -> "F"
                                    HeroType.ARES_BISHOP -> if (piece.ultimateCharge() >= snapshot.heroRules().ares().revengeCaptures()) "A●" else "A"
                                    HeroType.GINSENG_BISHOP -> if (piece.storedPieceId() != null) "G●" else "G"
                                    HeroType.IZBA_ROOK -> "И"
                                        HeroType.BABA_YAGA_PAWN -> "Я"
                                        HeroType.BOLOTNIK_KNIGHT -> "Б"
                                    else -> null
                                },
                                lightningLabel = if (visible && actualPiece != null) snapshot.lightning().charges().entries
                                    .filter { it.key.targetId() == actualPiece.id() }.sortedBy { it.key.casterId() }
                                    .joinToString(" ") { (if (snapshot.pieces()[it.key.casterId()]?.hero() == HeroType.ZEUS_ROOK) "Z" else "G") + it.value.count() }.ifEmpty { null } else null,
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
                                        if (selectedAbility == Ability.DISPLACE && displaced == null) {
                                            displacementTargetId = piece?.id(); moveError = null
                                        } else {
                                            val request = AbilityRequest(selectedPos, selectedAbility,
                                                if (selectedAbility == Ability.DISPLACE) displaced!!.position() else square,
                                                if (selectedAbility == Ability.DISPLACE) square else null, selectedBorrowed)
                                            val result = game.useAbility(currentPlayer, request)
                                            selectedAbility = null; selectedBorrowed = false; displacementTargetId = null
                                            updateView(result)
                                        }
                                    } else if (selectedAbility == null && selectedPos != null && (isMove || isCapture)) {
                                        submitMove(selectedPos, square)
                                    } else if (square == selectedPos) clearSelection()
                                    else if (selectedAbility == null && selectedPos != null && !own && FaunView.hasHiddenEnemy(snapshot)) {
                                        submitMove(selectedPos,square) // Blind capture attempts use the real board.
                                    } else if (own) {
                                        selectedId = piece?.id(); selectedAbility = null; selectedBorrowed = false; displacementTargetId = null; moveError = null
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
                    Text(if (selected.hero() == HeroType.NONE) selected.type().simpleName else selected.hero().getName(),
                        fontWeight = FontWeight.Bold)
                    if (selected.isBlinded) Text("Ослепление: ${selected.blindTurns()} ход владельца. Взятия недоступны.", fontSize = 12.sp, color = Color(0xFFC65D00))
                    if (selected.hero() == HeroType.BABA_YAGA_PAWN) {
                        val origin = snapshot.summons().records()[selected.id()]!!
                        val remaining = if (origin.bornTurn() == snapshot.turn().number() && selected.color() == snapshot.active())
                            snapshot.heroRules().izba().lifetimeTurns().toLong()
                            else ((origin.transformsAfterTurn() - snapshot.turn().number()) / 2 + 1).coerceAtLeast(1)
                        Text("Станет Избой: осталось завершить своих ходов — $remaining", fontSize = 13.sp)
                    }
                    if (selected.hero() != HeroType.NONE && selected.hero() != HeroType.BABA_YAGA_PAWN && selected.hero() != HeroType.IZBA_ROOK && selected.hero() != HeroType.GINSENG_BISHOP && selected.hero() != HeroType.FAUN_KNIGHT && selected.hero() != HeroType.ARES_BISHOP && selected.hero() != HeroType.CLOWNGANGER_KNIGHT && selected.hero() != HeroType.ILLUSION && selected.hero() != HeroType.MOLEMINER_PAWN && selected.hero() != HeroType.ZEUS_ROOK && selected.hero() != HeroType.CHRONOMLIN_PAWN && selected.hero() != HeroType.KIKIMORA_BISHOP) {
                        val pudge = selected.hero() == HeroType.PUDGE_ROOK
                        val lume = selected.hero() == HeroType.LUMESHARD_BISHOP
                        val crow = selected.hero() == HeroType.BOUNTY_CROW_PAWN
                        val skill = if (pudge) Ability.HOOK else if (lume) Ability.LASER else if (crow) Ability.BOUNTY else Ability.MUD
                        val ult = if (pudge) Ability.SWALLOW else if (lume) Ability.TELEPORT else if (crow) Ability.STEAL else Ability.AMBUSH
                        val skillName = if (pudge) "Хук" else if (lume) "Лазер" else if (crow) "Награда" else "Грязь"
                        val ultName = if (pudge) "Проглотить" else if (lume) "Телепорт" else if (crow) "Украсть" else "Засада"
                        val skillReady = game.getAbilityAvailability(selectedPos, skill)
                        val ultReady = game.getAbilityAvailability(selectedPos, ult)
                        val threshold = if (pudge) snapshot.pudgeRules().ultimateCaptures() else if (lume) snapshot.lumeshardRules().ultimateCaptures() else if (crow) snapshot.crowRules().ultimateCaptures() else snapshot.bolotnikRules().ultimateCaptures()
                        Text("$skillName: ${if (selected.cooldown() == 0) "готов" else "КД ${selected.cooldown()}"} • ${if (pudge || lume) "радиус 3" else if (crow) "вся доска, +1,5 золота" else "радиус 4"}", fontSize = 13.sp)
                        Text("Ульта: ${if (threshold == 0) "без заряда" else "${selected.ultimateCharge()} / $threshold"} • ${if (pudge) "радиус 1 вокруг" else if (lume) "свободная клетка своего цвета" else if (crow) "украсть до 2 золота" else "клетка и соседи слева/справа"}", fontSize = 13.sp)
                        if (lume) Text("Исходный цвет: ${if (selected.startingColor() == SquareColor.DARK) "чёрный" else "белый"}", fontSize = 13.sp)
                        if (crow) Text("Пассивка: +0,5 золота за собственное взятие", fontSize = 13.sp)
                        if (!pudge && !lume && !crow) Text("Враг входит в болото — Bolotnik выходит и берёт его", fontSize = 12.sp)
                        selected.storedPieceId()?.let { passenger ->
                            Text("Внутри: ${snapshot.pieces()[passenger]?.type()?.simpleName}", color = Color(0xFF6A5ACD))
                        }
                        Row {
                            Button(enabled = skillReady.isSuccess, onClick = { chooseAbility(skill) }) { Text(skillName) }
                            Spacer(Modifier.width(8.dp))
                            Button(enabled = ultReady.isSuccess, onClick = { if (ult == Ability.STEAL || ult == Ability.AMBUSH) { selectedAbility = null; updateView(game.useAbility(currentPlayer, selectedPos, null, ult)) } else chooseAbility(ult) }) { Text(ultName) }
                        }
                        if (selectedAbility != null) Text("Выберите подсвеченную цель", color = Color(0xFF6A5ACD))
                        if (!skillReady.isSuccess && skillReady != MoveResult.ABILITY_ALREADY_USED) Text(skillReady.message, fontSize = 12.sp, color = Color.Gray)
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
                } else if (moba) Text("Выберите свою фигуру. Пудж — П, Lumeshard — L, Crow — C, Bolotnik — Б, Изба — И, Ginseng — G, Faun — F, Ares — A, Clownganger — Кл, Moleminer — M, Zeus — Z, Chronomlin — Ch, Kikimora — Ки.", fontSize = 13.sp, color = Color.Gray)
            }
            val hidden = snapshot.pieces().values.filter { it.status() == PieceState.Status.HIDDEN_BOG }
            if (hidden.isNotEmpty()) Text("В болоте: " + hidden.joinToString { if (it.color() == FigureColor.WHITE) if (it.hero() == HeroType.GINSENG_BISHOP) "белый Ginseng" else "белый Bolotnik" else if (it.hero() == HeroType.GINSENG_BISHOP) "чёрный Ginseng" else "чёрный Bolotnik" } + ". Выход только при срабатывании.", fontSize = 12.sp, color = Color(0xFF275D38))
            if(moba && selected?.hero()==HeroType.KIKIMORA_BISHOP){
                Text("Пряжа: КД ${selected.cooldown()}. Горшок: ${selected.ultimateCharge()} / ${snapshot.heroRules().kikimora().ultimateCaptures()}",fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.YARN).isSuccess,onClick={chooseAbility(Ability.YARN)}){Text("Связать пряжей")}
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.CLAY_POT).isSuccess,onClick={chooseAbility(Ability.CLAY_POT)}){Text("Глиняный горшок")}
                }
                if(selectedAbility==Ability.YARN)Text("Враг в радиусе 2. Другая фигура за целью по направлению броска усиливает стан до 2 ходов.",fontSize=13.sp)
                if(selectedAbility==Ability.CLAY_POT)Text("Любая соседняя фигура, кроме короля: вечное неуязвимое препятствие. Можно сдвинуть способностью.",fontSize=13.sp)
            }
            if(moba && selected?.hero()==HeroType.CHRONOMLIN_PAWN){
                Text("Притяжение: КД ${selected.cooldown()}. Улучшение: ${selected.ultimateCharge()} / ${snapshot.heroRules().chronomlin().ultimateCaptures()}",fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.GRAPPLE).isSuccess,onClick={chooseAbility(Ability.GRAPPLE)}){Text("Притянуться")}
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.EVOLVE).isSuccess,onClick={val actor=selectedPos;clearSelection();updateView(game.useAbility(currentPlayer,actor,null,Ability.EVOLVE))}){Text("Улучшиться")}
                }
                Text("Пешка → слон → конь → ладья. Способности сохраняются.",fontSize=13.sp)
                if(selectedAbility==Ability.GRAPPLE)Text("Выберите первую фигуру по прямой, включая короля. Приземление перед ней, без взятия.",fontSize=13.sp)
            }
            if(moba && selected?.hero()==HeroType.ZEUS_ROOK){
                Text("Молния: КД ${selected.cooldown()}. Ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().zeus().ultimateCaptures()}",fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.CHAIN_LIGHTNING).isSuccess,onClick={chooseAbility(Ability.CHAIN_LIGHTNING)}){Text("Цепная молния")}
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.THUNDERSTORM).isSuccess,onClick={val actor=selectedPos;clearSelection();updateView(game.useAbility(currentPlayer,actor,null,Ability.THUNDERSTORM))}){Text("Разряд")}
                }
                Text("Третий заряд убивает. Разряд выбирает случайного врага с зарядом этого героя. Z и G — разные заряды.",fontSize=13.sp)
                if(selectedAbility==Ability.CHAIN_LIGHTNING)Text("Выберите врага в радиусе 3; цепь поразит до 3 целей.",fontSize=13.sp)
            }
            if(moba && selected?.hero()==HeroType.MOLEMINER_PAWN){
                Text("Мина: КД ${selected.cooldown()}. Ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().moleminer().ultimateCaptures()}",fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.PLANT_MINE).isSuccess,onClick={val actor=selectedPos;clearSelection();updateView(game.useAbility(currentPlayer,actor,null,Ability.PLANT_MINE))}){Text("Заложить мину")}
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.DETONATE).isSuccess,onClick={chooseAbility(Ability.DETONATE)}){Text("Прыжок и взрыв")}
                }
                if(selectedAbility==Ability.DETONATE)Text("Клетка в радиусе 3. Случайный квадрат 2×2: погибнут герой и все внутри, кроме королей и фигур в коробках.",fontSize=13.sp)
            }
            if(moba && selected?.hero()==HeroType.CLOWNGANGER_KNIGHT){
                Text("Коробка: радиус 3, КД ${selected.cooldown()}. Ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().clownganger().ultimateCaptures()}",fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.BOX).isSuccess,onClick={chooseAbility(Ability.BOX)}){Text("Спрятать в коробку")}
                    Button(enabled=game.getAbilityAvailability(selectedPos,Ability.ILLUSION).isSuccess,onClick={chooseAbility(Ability.ILLUSION)}){Text("Создать иллюзию")}
                }
                if(selectedAbility==Ability.BOX)Text("Выберите союзника, себя или иллюзию в радиусе 3",fontSize=13.sp)
                if(selectedAbility==Ability.ILLUSION)Text("Выберите свободную соседнюю клетку",fontSize=13.sp)
            }
            if(moba && selected?.hero()==HeroType.ILLUSION)Text("Иллюзия: не берёт фигуры. Может сходить вместе с оригиналом.",fontSize=13.sp)
            if(moba && selected?.isBoxed==true)Text("В коробке: защищена и не действует до конца следующего собственного хода",fontSize=13.sp)
            if (moba && selected?.hero() == HeroType.ARES_BISHOP) {
                Text("Копьё: 3 прямо / 2 по диагонали • КД ${selected.cooldown()}",fontSize=13.sp)
                Text("Пассивная ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().ares().revengeCaptures()} • заберёт взявшую его фигуру",fontSize=13.sp)
                val ready=game.getAbilityAvailability(selectedPos,Ability.SPEAR)
                Button(enabled=ready.isSuccess,onClick={chooseAbility(Ability.SPEAR)}){Text("Бросить копьё")}
                if(selectedAbility==Ability.SPEAR)Text("Выберите подсвеченное направление: первая фигура летит до конца, столкновение оглушает обе",fontSize=13.sp)
                if(!ready.isSuccess)Text(ready.message,fontSize=12.sp,color=Color.Gray)
            }
            if (moba && selected?.isStunned == true) Text("Оглушён до конца следующего хода соперника источника",fontSize=12.sp,color=Color(0xFF8A4F00))
            if (moba && selected?.hero() == HeroType.FAUN_KNIGHT) {
                Text("Туча 3×3 • дальность 4 • КД ${selected.cooldown()}",fontSize=13.sp)
                Text("Невидимость: ${selected.ultimateCharge()} / ${snapshot.heroRules().faun().ultimateCaptures()} • 2 следующих своих хода",fontSize=13.sp)
                val cloudReady=game.getAbilityAvailability(selectedPos,Ability.CLOUD)
                val invisReady=game.getAbilityAvailability(selectedPos,Ability.INVISIBILITY)
                Row {
                    Button(enabled=cloudReady.isSuccess,onClick={chooseAbility(Ability.CLOUD)}){Text("Бросить тучу")}
                    Spacer(Modifier.width(8.dp))
                    Button(enabled=invisReady.isSuccess,onClick={val actor=selectedPos;clearSelection();updateView(game.useAbility(currentPlayer,actor,null,Ability.INVISIBILITY))}){Text("Невидимость")}
                }
                if(snapshot.faun().silenced(selectedPos))Text("В туче: скилл и ульта недоступны",fontSize=12.sp,color=Color.Gray)
            }
            if (moba && selected?.hero() == HeroType.IZBA_ROOK) {
                Text("Пассивка: при взятии призовёт Бабу-ягу на свободную клетку рядом", fontSize = 13.sp)
                Text("Ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().izba().ultimateCaptures()}", fontSize = 13.sp)
                val ready=game.getAbilityAvailability(selectedPos,Ability.SUMMON_YAGA)
                Button(enabled=ready.isSuccess,onClick={clearSelection();updateView(game.useAbility(currentPlayer,selectedPos,null,Ability.SUMMON_YAGA))}) { Text("Призвать Бабу-ягу") }
                if(!ready.isSuccess)Text(ready.message,fontSize=12.sp,color=Color.Gray)
            }
            if (moba && selected?.hero() == HeroType.GINSENG_BISHOP) {
                val normal = game.getAbilityAvailability(selectedPos, Ability.DISPLACE)
                val ultimate = game.getAbilityAvailability(selectedPos, Ability.COPY_SPELL)
                Text("Сдвиг: радиус 3 • КД ${selected.cooldown()} • новая клетка рядом с целью", fontSize = 13.sp)
                Text("Ульта: ${selected.ultimateCharge()} / ${snapshot.heroRules().ginseng().ultimateCaptures()} • последняя активная способность цели", fontSize = 13.sp)
                Row {
                    Button(enabled = normal.isSuccess, onClick = { chooseAbility(Ability.DISPLACE) }) { Text("Переместить фигуру") }
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = ultimate.isSuccess, onClick = { chooseAbility(Ability.COPY_SPELL) }) { Text("Копировать последнее") }
                }
                val copy = snapshot.spells().borrowed()[selected.id()]
                if (copy != null) {
                    val ready = game.getAbilityAvailability(selectedPos, copy.ability(), true)
                    Text("Копия: ${copy.ability().displayName} • одно применение", fontSize = 13.sp)
                    Button(enabled = ready.isSuccess, onClick = {
                        if (copy.ability() in listOf(Ability.STEAL, Ability.AMBUSH, Ability.SUMMON_YAGA, Ability.INVISIBILITY, Ability.PLANT_MINE, Ability.THUNDERSTORM, Ability.EVOLVE)) {
                            val request = AbilityRequest(selectedPos, copy.ability(), null, null, true)
                            clearSelection(); updateView(game.useAbility(currentPlayer, request))
                        } else chooseAbility(copy.ability(), true)
                    }) { Text("Использовать: ${copy.ability().displayName}") }
                    if (!ready.isSuccess) Text(ready.message, fontSize = 12.sp, color = Color.Gray)
                } else Text("Украденной способности нет", fontSize = 12.sp, color = Color.Gray)
                if (selectedAbility == Ability.DISPLACE) {
                    Text(if (displaced == null) "Выберите подсвеченную фигуру" else "Выберите свободную клетку рядом с выбранной фигурой", fontSize = 13.sp)
                    if (displaced != null) Button(onClick = { displacementTargetId = null }) { Text("Выбрать другую цель") }
                }
                if (selectedAbility == Ability.COPY_SPELL) Text("Выберите героя: скопируется последняя успешно применённая им способность", fontSize = 13.sp)
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
