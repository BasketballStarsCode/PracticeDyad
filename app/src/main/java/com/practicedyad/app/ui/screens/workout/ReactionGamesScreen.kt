package com.practicedyad.app.ui.screens.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.practicedyad.app.ui.components.PDButton
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.viewmodel.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import kotlin.random.Random

private enum class GamePhase { COUNTDOWN, PLAYING, RESULT, DONE }

private val gameColors = listOf(
    Color(0xFFF44336) to "Rot",
    Color(0xFF2196F3) to "Blau",
    Color(0xFF4CAF50) to "Grün",
    Color(0xFFFFEB3B) to "Gelb",
    Color(0xFF9C27B0) to "Lila",
    Color(0xFFFF9800) to "Orange",
    Color(0xFF00BCD4) to "Türkis"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionGamesScreen(
    navController: NavController,
    exerciseType: String,
    exerciseId: String,
    roundSeconds: Int,
    rounds: Int,
    param2: Int,
    param3: Int,
    workoutVm: WorkoutViewModel = hiltViewModel()
) {
    val title = when (exerciseType) {
        "reaction_tap" -> "Schnell Antippen"
        "circle_overlap" -> "Kreise treffen"
        "color_reaction" -> "Reaktion auf Farben"
        else -> "Spiel"
    }

    var currentRound by remember { mutableStateOf(1) }
    var totalDone by remember { mutableStateOf(false) }

    // Accumulated results for final save
    var totalReactionSumMs by remember { mutableStateOf(0L) }
    var totalReactionCount by remember { mutableStateOf(0) }
    var totalCorrect by remember { mutableStateOf(0) }
    var totalWrong by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (totalDone) {
                GameSummary(
                    exerciseType = exerciseType,
                    rounds = rounds,
                    totalReactionSumMs = totalReactionSumMs,
                    totalReactionCount = totalReactionCount,
                    totalCorrect = totalCorrect,
                    totalWrong = totalWrong,
                    onDone = { navController.popBackStack() }
                )
            } else {
                when (exerciseType) {
                    "reaction_tap" -> ReactionTapGame(
                        roundSeconds = roundSeconds,
                        currentRound = currentRound,
                        totalRounds = rounds,
                        onRoundDone = { avgMs ->
                            if (avgMs > 0) {
                                totalReactionSumMs += avgMs
                                totalReactionCount++
                            }
                            if (currentRound >= rounds) {
                                val overallAvg = if (totalReactionCount > 0)
                                    totalReactionSumMs / totalReactionCount else 0L
                                workoutVm.saveGameResult(
                                    exerciseId = exerciseId,
                                    exerciseName = "Schnell Antippen",
                                    avgReactionMs = overallAvg
                                )
                                totalDone = true
                            } else {
                                currentRound++
                            }
                        }
                    )
                    "circle_overlap" -> CircleOverlapGame(
                        roundSeconds = roundSeconds,
                        circleCount = param2.coerceAtLeast(1),
                        overlapRequired = param3.coerceAtLeast(1),
                        currentRound = currentRound,
                        totalRounds = rounds,
                        onRoundDone = { correct, wrong ->
                            totalCorrect += correct
                            totalWrong += wrong
                            if (currentRound >= rounds) {
                                workoutVm.saveGameResult(
                                    exerciseId = exerciseId,
                                    exerciseName = "Kreise treffen",
                                    correctAttempts = totalCorrect,
                                    wrongAttempts = totalWrong
                                )
                                totalDone = true
                            } else {
                                currentRound++
                            }
                        }
                    )
                    "color_reaction" -> ColorReactionGame(
                        roundSeconds = roundSeconds,
                        avgIntervalSeconds = param2.coerceAtLeast(1),
                        colorCount = param3.coerceIn(1, 7),
                        currentRound = currentRound,
                        totalRounds = rounds,
                        onRoundDone = {
                            if (currentRound >= rounds) {
                                totalDone = true
                            } else {
                                currentRound++
                            }
                        }
                    )
                    else -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Unbekannter Spieltyp")
                        }
                    }
                }
            }
        }
    }
}

// ─── Summary screen ────────────────────────────────────────────────────────────

@Composable
private fun GameSummary(
    exerciseType: String,
    rounds: Int,
    totalReactionSumMs: Long,
    totalReactionCount: Int,
    totalCorrect: Int,
    totalWrong: Int,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Übung abgeschlossen!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("$rounds Runden", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        when (exerciseType) {
            "reaction_tap" -> {
                val avg = if (totalReactionCount > 0) totalReactionSumMs / totalReactionCount else 0L
                StatCard("Ø Reaktionszeit", "${avg} ms")
                Spacer(Modifier.height(8.dp))
                StatCard("Gemessene Reaktionen", "$totalReactionCount")
            }
            "circle_overlap" -> {
                StatCard("Richtige Versuche", "$totalCorrect")
                Spacer(Modifier.height(8.dp))
                StatCard("Falsche Versuche", "$totalWrong")
                if (totalCorrect + totalWrong > 0) {
                    Spacer(Modifier.height(8.dp))
                    val pct = (totalCorrect * 100) / (totalCorrect + totalWrong)
                    StatCard("Genauigkeit", "$pct %")
                }
            }
        }
        Spacer(Modifier.height(40.dp))
        PDButton("Fertig", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TealPrimary)
        }
    }
}

// ─── Countdown composable ──────────────────────────────────────────────────────

@Composable
private fun CountdownOverlay(onDone: () -> Unit) {
    var count by remember { mutableStateOf(3) }
    LaunchedEffect(Unit) {
        while (count > 0) {
            delay(1000)
            count--
        }
        onDone()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (count > 0) "$count" else "Los!",
            fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ─── Round result overlay ──────────────────────────────────────────────────────

@Composable
private fun RoundResultOverlay(
    currentRound: Int,
    totalRounds: Int,
    resultLines: List<Pair<String, String>>,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                "Runde $currentRound abgeschlossen",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            resultLines.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color.LightGray, fontSize = 16.sp)
                    Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (currentRound >= totalRounds) "Fertig" else "Nächste Runde",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── Game 1: Schnell Antippen ──────────────────────────────────────────────────

@Composable
private fun ReactionTapGame(
    roundSeconds: Int,
    currentRound: Int,
    totalRounds: Int,
    onRoundDone: (avgReactionMs: Long) -> Unit
) {
    var phase by remember(currentRound) { mutableStateOf(GamePhase.COUNTDOWN) }
    var isFlashing by remember(currentRound) { mutableStateOf(false) }
    var flashStartTime by remember(currentRound) { mutableStateOf(0L) }
    var timeLeftMs by remember(currentRound) { mutableStateOf(roundSeconds * 1000L) }
    val reactionTimes = remember(currentRound) { mutableStateListOf<Long>() }
    var roundAvgMs by remember(currentRound) { mutableStateOf(0L) }

    LaunchedEffect(currentRound, phase) {
        if (phase != GamePhase.PLAYING) return@LaunchedEffect
        // Timer
        launch {
            while (timeLeftMs > 0 && phase == GamePhase.PLAYING) {
                delay(100)
                timeLeftMs -= 100
            }
            if (phase == GamePhase.PLAYING) {
                isFlashing = false
                roundAvgMs = if (reactionTimes.isNotEmpty()) reactionTimes.average().toLong() else 0L
                phase = GamePhase.RESULT
            }
        }
        // Flash loop
        launch {
            while (phase == GamePhase.PLAYING) {
                val waitMs = Random.nextLong(500, 3000)
                delay(waitMs)
                if (phase != GamePhase.PLAYING) break
                isFlashing = true
                flashStartTime = System.currentTimeMillis()
                delay(400)
                if (isFlashing) isFlashing = false
            }
        }
    }

    val bgColor = if (isFlashing) Color.White else Color.Black
    val textColor = if (isFlashing) Color.Black else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(currentRound, phase) {
                detectTapGestures {
                    if (phase == GamePhase.PLAYING && isFlashing) {
                        val reactionMs = System.currentTimeMillis() - flashStartTime
                        reactionTimes.add(reactionMs)
                        isFlashing = false
                    }
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        if (phase == GamePhase.PLAYING) {
            Column(
                modifier = Modifier.padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Runde $currentRound / $totalRounds",
                    color = textColor.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(timeLeftMs / 1000)}s",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                if (reactionTimes.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Letzte: ${reactionTimes.last()} ms",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (phase == GamePhase.COUNTDOWN) {
            CountdownOverlay(onDone = { phase = GamePhase.PLAYING })
        }

        if (phase == GamePhase.RESULT) {
            val resultLines = buildList {
                if (reactionTimes.isNotEmpty()) {
                    add("Ø Reaktionszeit" to "${roundAvgMs} ms")
                    add("Schnellste Reaktion" to "${reactionTimes.min()} ms")
                    add("Anzahl Reaktionen" to "${reactionTimes.size}")
                } else {
                    add("Reaktionen" to "keine gemessen")
                }
            }
            RoundResultOverlay(
                currentRound = currentRound,
                totalRounds = totalRounds,
                resultLines = resultLines,
                onNext = { onRoundDone(roundAvgMs) }
            )
        }
    }
}

// ─── Game 2: Kreise treffen ────────────────────────────────────────────────────

private data class GameCircle(
    val x: Float, val y: Float,
    val vx: Float, val vy: Float,
    val filled: Boolean
)

@Composable
private fun CircleOverlapGame(
    roundSeconds: Int,
    circleCount: Int,
    overlapRequired: Int,
    currentRound: Int,
    totalRounds: Int,
    onRoundDone: (correct: Int, wrong: Int) -> Unit
) {
    var phase by remember(currentRound) { mutableStateOf(GamePhase.COUNTDOWN) }
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }
    var circles by remember(currentRound) { mutableStateOf<List<GameCircle>>(emptyList()) }
    var correctCount by remember(currentRound) { mutableStateOf(0) }
    var wrongCount by remember(currentRound) { mutableStateOf(0) }
    var timeLeftMs by remember(currentRound) { mutableStateOf(roundSeconds * 1000L) }
    var overlappingCount by remember { mutableStateOf(0) }

    val density = LocalDensity.current
    val filledRadiusPx = with(density) { 20.dp.toPx() }
    val unfilledRadiusPx = with(density) { 30.dp.toPx() }

    fun initCircles(size: Size) {
        val filled = (1..circleCount).map {
            GameCircle(
                x = Random.nextFloat() * (size.width - 2 * filledRadiusPx) + filledRadiusPx,
                y = Random.nextFloat() * (size.height / 2),
                vx = 0f,
                vy = Random.nextFloat() * 3f + 1.5f,
                filled = true
            )
        }
        val unfilled = (1..circleCount).map {
            GameCircle(
                x = Random.nextFloat() * (size.width - 2 * unfilledRadiusPx) + unfilledRadiusPx,
                y = Random.nextFloat() * (size.height - 2 * unfilledRadiusPx) + unfilledRadiusPx,
                vx = (Random.nextFloat() * 3f + 1.5f) * if (Random.nextBoolean()) 1f else -1f,
                vy = 0f,
                filled = false
            )
        }
        circles = filled + unfilled
    }

    fun updateCircles(size: Size): List<GameCircle> {
        return circles.map { c ->
            if (c.filled) {
                var ny = c.y + c.vy
                if (ny > size.height + filledRadiusPx) {
                    ny = -filledRadiusPx
                }
                c.copy(y = ny)
            } else {
                var nx = c.x + c.vx
                var nvx = c.vx
                if (nx < unfilledRadiusPx || nx > size.width - unfilledRadiusPx) {
                    nvx = -nvx
                    nx = c.x + nvx
                }
                c.copy(x = nx, vx = nvx)
            }
        }
    }

    fun countOverlaps(cs: List<GameCircle>): Int {
        val filledList = cs.filter { it.filled }
        val unfilledList = cs.filter { !it.filled }
        return filledList.count { f ->
            unfilledList.any { u ->
                val dx = f.x - u.x
                val dy = f.y - u.y
                sqrt(dx * dx + dy * dy) < filledRadiusPx + unfilledRadiusPx
            }
        }
    }

    LaunchedEffect(currentRound, phase, canvasWidth) {
        if (phase != GamePhase.PLAYING || canvasWidth == 0f) return@LaunchedEffect
        if (circles.isEmpty()) initCircles(Size(canvasWidth, canvasHeight))

        val timerJob = launch {
            while (timeLeftMs > 0 && phase == GamePhase.PLAYING) {
                delay(100)
                timeLeftMs -= 100
            }
            if (phase == GamePhase.PLAYING) phase = GamePhase.RESULT
        }
        val animJob = launch {
            while (phase == GamePhase.PLAYING) {
                delay(16)
                circles = updateCircles(Size(canvasWidth, canvasHeight))
                overlappingCount = countOverlaps(circles)
            }
        }
        timerJob.join()
        animJob.cancel()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .pointerInput(currentRound, phase) {
                detectTapGestures {
                    if (phase == GamePhase.PLAYING) {
                        if (overlappingCount >= overlapRequired) correctCount++
                        else wrongCount++
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (canvasWidth == 0f) {
                canvasWidth = size.width
                canvasHeight = size.height
            }
            circles.forEach { c ->
                if (c.filled) {
                    drawCircle(
                        color = TealPrimary,
                        radius = filledRadiusPx,
                        center = Offset(c.x, c.y)
                    )
                } else {
                    drawCircle(
                        color = Color(0xFFE53935),
                        radius = unfilledRadiusPx,
                        center = Offset(c.x, c.y),
                        style = Stroke(width = 4f)
                    )
                }
            }
        }

        if (phase == GamePhase.PLAYING) {
            Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Runde $currentRound / $totalRounds", fontSize = 13.sp, color = Color.Gray)
                Text("${(timeLeftMs / 1000)}s", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                val isReady = overlappingCount >= overlapRequired
                Surface(
                    color = if (isReady) Color(0xFF4CAF50).copy(alpha = 0.9f) else Color.Gray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isReady) "JETZT TIPPEN! ($overlappingCount/$overlapRequired)" else "Warten... ($overlappingCount/$overlapRequired)",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("✓ $correctCount  ✗ $wrongCount", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        if (phase == GamePhase.COUNTDOWN) {
            CountdownOverlay(onDone = {
                if (canvasWidth > 0f) initCircles(Size(canvasWidth, canvasHeight))
                phase = GamePhase.PLAYING
            })
        }

        if (phase == GamePhase.RESULT) {
            val total = correctCount + wrongCount
            val pct = if (total > 0) (correctCount * 100) / total else 0
            RoundResultOverlay(
                currentRound = currentRound,
                totalRounds = totalRounds,
                resultLines = listOf(
                    "Richtige Versuche" to "$correctCount",
                    "Falsche Versuche" to "$wrongCount",
                    "Genauigkeit" to "$pct %"
                ),
                onNext = { onRoundDone(correctCount, wrongCount) }
            )
        }
    }
}

// ─── Game 3: Reaktion auf Farben ──────────────────────────────────────────────

@Composable
private fun ColorReactionGame(
    roundSeconds: Int,
    avgIntervalSeconds: Int,
    colorCount: Int,
    currentRound: Int,
    totalRounds: Int,
    onRoundDone: () -> Unit
) {
    var phase by remember(currentRound) { mutableStateOf(GamePhase.COUNTDOWN) }
    val safeColorCount = colorCount.coerceIn(1, gameColors.size)
    val activeColors = remember(safeColorCount) { gameColors.take(safeColorCount) }
    var currentColorIdx by remember(currentRound) { mutableStateOf(0) }
    var timeLeftMs by remember(currentRound) { mutableStateOf(roundSeconds * 1000L) }

    LaunchedEffect(currentRound, phase) {
        if (phase != GamePhase.PLAYING) return@LaunchedEffect
        val timerJob = launch {
            while (timeLeftMs > 0) {
                delay(100)
                timeLeftMs -= 100
            }
            phase = GamePhase.RESULT
        }
        val colorJob = launch {
            while (phase == GamePhase.PLAYING) {
                val intervalMs = (avgIntervalSeconds * 1000L * (Random.nextFloat() * 0.6f + 0.7f)).toLong()
                delay(intervalMs)
                if (phase != GamePhase.PLAYING) break
                currentColorIdx = (currentColorIdx + 1 + Random.nextInt(activeColors.size - 1).coerceAtLeast(0)) % activeColors.size
            }
        }
        timerJob.join()
        colorJob.cancel()
    }

    val (bgColor, colorName) = activeColors[currentColorIdx]

    Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
        if (phase == GamePhase.PLAYING) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    colorName,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "Runde $currentRound / $totalRounds  ·  ${timeLeftMs / 1000}s",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        if (phase == GamePhase.COUNTDOWN) {
            CountdownOverlay(onDone = { phase = GamePhase.PLAYING })
        }

        if (phase == GamePhase.RESULT) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        "Runde $currentRound abgeschlossen",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onRoundDone,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (currentRound >= totalRounds) "Fertig" else "Nächste Runde",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
