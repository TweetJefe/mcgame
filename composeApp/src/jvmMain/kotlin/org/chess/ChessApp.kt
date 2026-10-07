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
import game.Game
import game.PudgeRules

@Composable
fun ChessApp(game: Game) {
    var currentScreen by remember { mutableStateOf(ChessScreen.MENU) }
    var activeGame by remember { mutableStateOf(game) }
    Box(Modifier.fillMaxSize()) {
        when (currentScreen) {
            ChessScreen.MENU -> MainMenu(
                onStartGame = { time ->
                    activeGame = Game(8, time)
                    currentScreen = ChessScreen.GAME
                },
                onStartMoba = { testing ->
                    activeGame = Game.mobaPrototype(PudgeRules(if (testing) 0 else 2, if (testing) 0 else 3, true, false), 600)
                    currentScreen = ChessScreen.GAME
                }
            )
            ChessScreen.GAME -> key(activeGame) {
                ChessBoard(activeGame) { currentScreen = ChessScreen.MENU }
            }
        }
    }
}

@Composable
fun MainMenu(onStartGame: (Int) -> Unit, onStartMoba: (Boolean) -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFF302E2B)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Moba Chess", fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp))
        MenuButton("Пудж-Ладья • 10 минут", Color(0xFF6A5ACD)) { onStartMoba(false) }
        MenuButton("Тест скиллов • без КД/заряда", Color(0xFF6A5ACD)) { onStartMoba(true) }
        Text("В тесте сохраняется один скилл или ульта за ход", color = Color.LightGray, fontSize = 13.sp)
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
