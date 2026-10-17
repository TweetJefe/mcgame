package org.chess


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import game.core.Game
import game.draft.Draft
import game.heroes.HeroRules
import game.scenarios.TrainingScenario
import game.scenarios.LumeshardScenario
import game.scenarios.BountyCrowScenario
import game.scenarios.BolotnikScenario
import game.scenarios.PudgeScenario
import game.persistence.GameSaveFile

@Composable
fun ChessApp(game: Game, filePicker: GameFilePicker = DesktopGameFilePicker) {
    var activeDraft by remember { mutableStateOf(Draft()) }
    var currentScreen by remember { mutableStateOf(ChessScreen.MENU) }
    var activeGame by remember { mutableStateOf(game) }
    var scenario by remember { mutableStateOf<TrainingScenario?>(null) }
    var fileMessage by remember { mutableStateOf<String?>(null) }
    fun loadGame() {
        try {
            val path = filePicker.choose(false) ?: return
            val loaded = GameSaveFile.load(path)
            activeGame = loaded
            scenario = null
            currentScreen = ChessScreen.GAME
            fileMessage = "Загружено: ${path.fileName}"
        } catch (error: Exception) {
            fileMessage = "Не удалось загрузить: ${error.message}"
        }
    }
    fun saveGame() {
        try {
            val path = filePicker.choose(true) ?: return
            GameSaveFile.save(path, activeGame)
            fileMessage = "Сохранено: ${path.fileName}"
        } catch (error: Exception) {
            fileMessage = "Не удалось сохранить: ${error.message}"
        }
    }
    Box(Modifier.fillMaxSize()) {
        when (currentScreen) {
            ChessScreen.MENU -> MainMenu(
                onStartGame = { time ->
                    scenario = null
                    fileMessage = null
                    activeGame = Game(8, time)
                    currentScreen = ChessScreen.GAME
                },
                onStartMoba = { testing ->
                    scenario = null
                    fileMessage = null
                    if (testing) {
                        activeGame = Game.mobaHeroesSandbox(600)
                        currentScreen = ChessScreen.GAME
                    } else {
                        activeDraft = Draft()
                        currentScreen = ChessScreen.DRAFT
                    }
                },
                onScenarios = { fileMessage = null; currentScreen = ChessScreen.SCENARIOS },
                onLumeshardScenarios = { fileMessage = null; currentScreen = ChessScreen.LUMESHARD_SCENARIOS },
                onCrowScenarios = { fileMessage = null; currentScreen = ChessScreen.CROW_SCENARIOS },
                onBolotnikScenarios = { fileMessage = null; currentScreen = ChessScreen.BOLOTNIK_SCENARIOS },
                onLoadGame = ::loadGame,
                fileMessage = fileMessage
            )
            ChessScreen.DRAFT -> DraftScreen(activeDraft,
                onStart = { completed ->
                    activeGame = Game.mobaFromDraft(completed, HeroRules.prototype(), 600)
                    scenario = null; fileMessage = null; currentScreen = ChessScreen.GAME
                }, onBack = { currentScreen = ChessScreen.MENU })
            ChessScreen.SCENARIOS -> PudgeScenariosMenu(
                onSelect = { selected ->
                    scenario = selected
                    fileMessage = null
                    activeGame = selected.createGame()
                    currentScreen = ChessScreen.GAME
                },
                onBack = { currentScreen = ChessScreen.MENU }
            )
            ChessScreen.LUMESHARD_SCENARIOS -> TrainingScenariosMenu("Тестовые позиции Lumeshard", LumeshardScenario.values().toList(),
                onSelect = { selected ->
                    scenario = selected; fileMessage = null; activeGame = selected.createGame(); currentScreen = ChessScreen.GAME
                }, onBack = { currentScreen = ChessScreen.MENU })
            ChessScreen.CROW_SCENARIOS -> TrainingScenariosMenu("Тестовые позиции Bounty Crow", BountyCrowScenario.values().toList(),
                onSelect = { selected ->
                    scenario = selected; fileMessage = null; activeGame = selected.createGame(); currentScreen = ChessScreen.GAME
                }, onBack = { currentScreen = ChessScreen.MENU })
            ChessScreen.BOLOTNIK_SCENARIOS -> TrainingScenariosMenu("Тестовые позиции Bolotnik", BolotnikScenario.values().toList(),
                onSelect = { selected -> scenario = selected; fileMessage = null; activeGame = selected.createGame(); currentScreen = ChessScreen.GAME },
                onBack = { currentScreen = ChessScreen.MENU })
            ChessScreen.GAME -> key(activeGame) {
                ChessBoard(activeGame, scenario = scenario,
                    onRestart = { scenario?.let { activeGame = it.createGame(); fileMessage = null } },
                    onSave = ::saveGame, onLoad = ::loadGame, fileMessage = fileMessage,
                    onNewGame = { currentScreen = if (scenario == null) ChessScreen.MENU else if (scenario is BolotnikScenario) ChessScreen.BOLOTNIK_SCENARIOS else if (scenario is BountyCrowScenario) ChessScreen.CROW_SCENARIOS else if (scenario is LumeshardScenario) ChessScreen.LUMESHARD_SCENARIOS else ChessScreen.SCENARIOS })
            }
        }
    }
}

@Composable
fun MainMenu(onStartGame: (Int) -> Unit, onStartMoba: (Boolean) -> Unit = {}, onScenarios: () -> Unit = {},
             onLoadGame: () -> Unit = {}, fileMessage: String? = null, onLumeshardScenarios: () -> Unit = {}, onCrowScenarios: () -> Unit = {}, onBolotnikScenarios: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF302E2B)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Moba Chess", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp))
        MenuButton("Moba Chess • пики и баны", Color(0xFF6A5ACD)) { onStartMoba(false) }
        MenuButton("Тест скиллов • без КД/заряда", Color(0xFF6A5ACD)) { onStartMoba(true) }
        MenuButton("Тестовые позиции Пуджа", Color(0xFF6A5ACD), onScenarios)
        MenuButton("Тестовые позиции Lumeshard", Color(0xFF6A5ACD), onLumeshardScenarios)
        MenuButton("Тестовые позиции Bounty Crow", Color(0xFF6A5ACD), onCrowScenarios)
        MenuButton("Тестовые позиции Bolotnik", Color(0xFF275D38), onBolotnikScenarios)
        Text("В тесте сохраняется один скилл или ульта за ход", color = Color.LightGray, fontSize = 13.sp)
        MenuButton("Загрузить партию", Color(0xFF56534E), onLoadGame)
        fileMessage?.let { Text(it, color = Color(0xFFFFB9B9), modifier = Modifier.widthIn(max = 700.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("Обычные шахматы", color = Color.White, fontSize = 18.sp)
        MenuButton("Быстрая партия • 3 минуты", Color(0xFF81B64C)) { onStartGame(180) }
        MenuButton("Средняя партия • 6 минут", Color(0xFF81B64C)) { onStartGame(360) }
        MenuButton("Длинная партия • 10 минут", Color(0xFF81B64C)) { onStartGame(600) }
    }
}

@Composable
fun MenuButton(text: String, color: Color, onClick: () -> Unit) {
    Button(onClick, colors = ButtonDefaults.buttonColors(backgroundColor = color),
        modifier = Modifier.padding(8.dp).width(360.dp).height(48.dp)) {
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
