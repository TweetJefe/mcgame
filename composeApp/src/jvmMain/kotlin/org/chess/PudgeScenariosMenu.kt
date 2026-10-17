package org.chess


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import game.scenarios.PudgeScenario
import game.scenarios.TrainingScenario

@Composable
fun PudgeScenariosMenu(onSelect: (PudgeScenario) -> Unit, onBack: () -> Unit) {
    TrainingScenariosMenu("Тестовые позиции Пуджа", PudgeScenario.values().toList(), { onSelect(it as PudgeScenario) }, onBack)
}

@Composable
fun TrainingScenariosMenu(title: String, scenarios: List<TrainingScenario>, onSelect: (TrainingScenario) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color(0xFF302E2B)).verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Без часов. За обе стороны играете вы. Любой сценарий можно начать заново.",
            color = Color.LightGray, modifier = Modifier.padding(vertical = 12.dp))
        scenarios.forEach { scenario ->
            Column(Modifier.widthIn(max = 700.dp).fillMaxWidth().padding(bottom = 12.dp)) {
                MenuButton(scenario.getTitle(), Color(0xFF6A5ACD)) { onSelect(scenario) }
                Text(scenario.getInstructions(), color = Color.LightGray, fontSize = 14.sp)
                Text(scenario.getSettingsLabel(),
                    color = Color(0xFFBBAAEF), fontSize = 12.sp)
            }
        }
        Button(onClick = onBack) { Text("Главное меню") }
    }
}
