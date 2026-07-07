package com.practicedyad.app.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.practicedyad.app.data.model.*
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.viewmodel.ChatViewModel
import com.practicedyad.app.viewmodel.TrainingPlanViewModel
import com.practicedyad.app.viewmodel.WorkoutViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkoutExecutionScreen(
    navController: NavController,
    workoutUnitId: String,
    planId: String,
    workoutVm: WorkoutViewModel = hiltViewModel(),
    planVm: TrainingPlanViewModel = hiltViewModel(),
    chatVm: ChatViewModel = hiltViewModel()
) {
    val plans by planVm.plans.collectAsStateWithLifecycle()
    val session by workoutVm.activeSession.collectAsStateWithLifecycle()
    val currentIndex by workoutVm.currentExerciseIndex.collectAsStateWithLifecycle()
    val lastWeights by workoutVm.lastWeights.collectAsStateWithLifecycle()
    val exercisePrefs by workoutVm.exercisePreferences.collectAsStateWithLifecycle()
    val loading by workoutVm.loading.collectAsStateWithLifecycle()

    val plan = plans.find { it.id == planId }
    val unit = plan?.workoutUnits?.find { it.id == workoutUnitId }

    var showFinishOptions by remember { mutableStateOf(false) }
    val conversationId by chatVm.conversationId.collectAsStateWithLifecycle()
    var chatRequested by remember { mutableStateOf(false) }

    // Circuit state
    var circuitRound by remember { mutableStateOf(1) }
    var circuitExerciseIdx by remember { mutableStateOf(0) }
    var showCircuitRest by remember { mutableStateOf(false) }
    var circuitRestSeconds by remember { mutableStateOf(0) }
    var circuitRestLabel by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(conversationId) {
        if (chatRequested && conversationId.isNotEmpty()) {
            chatRequested = false
            navController.navigate(Screen.Chat.createRoute(conversationId, plan?.coachId ?: ""))
        }
    }

    LaunchedEffect(plan, unit) {
        if (plan != null && unit != null && session == null) {
            workoutVm.startSession(unit, plan)
            workoutVm.loadExercisePreferences()
        }
    }

    LaunchedEffect(Unit) { planVm.loadCoachPlans() }

    if (unit == null || session == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TealPrimary)
        }
        return
    }

    val exercises = unit.exercises
    val freeOrder = unit.athleteChoosesOrder
    val s = LocalAppStrings.current

    if (showFinishOptions && unit.allowShareWorkout) {
        FinishDialog(
            onShare = { workoutVm.finishSession(true) { navController.navigate("home") { popUpTo(0) } } },
            onNoShare = { workoutVm.finishSession(false) { navController.navigate("home") { popUpTo(0) } } }
        )
        return
    }

    Scaffold(
        topBar = {
            PDTopBar(
                title = unit.name,
                onBack = {
                    workoutVm.finishSession(false) {}
                    navController.popBackStack()
                },
                actions = {
                    IconButton(onClick = {
                        plan?.coachId?.let { coachId ->
                            chatRequested = true
                            chatVm.openConversation(coachId)
                        }
                    }) {
                        Icon(Icons.Default.Chat, "Coach fragen", tint = TealPrimary)
                    }
                }
            )
        },
        bottomBar = {
            if (unit.isCircuit && !showCircuitRest) {
                val isLastExercise = circuitExerciseIdx == exercises.size - 1
                val isLastRound = circuitRound == unit.circuitRounds
                PDButton(
                    text = when {
                        isLastExercise && isLastRound -> "Fertig"
                        isLastExercise -> "Nächste Runde"
                        else -> "Nächste Übung"
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    onClick = {
                        when {
                            isLastExercise && isLastRound -> {
                                if (unit.allowShareWorkout) showFinishOptions = true
                                else workoutVm.finishSession(false) { navController.navigate("home") { popUpTo(0) } }
                            }
                            isLastExercise -> {
                                // Rest between rounds, then next round
                                if (unit.circuitRestBetweenRounds > 0) {
                                    circuitRestLabel = "Pause zwischen Runden"
                                    circuitRestSeconds = unit.circuitRestBetweenRounds
                                    showCircuitRest = true
                                    scope.launch {
                                        while (circuitRestSeconds > 0) {
                                            kotlinx.coroutines.delay(1000)
                                            circuitRestSeconds--
                                        }
                                        showCircuitRest = false
                                        circuitExerciseIdx = 0
                                        circuitRound++
                                    }
                                } else {
                                    circuitExerciseIdx = 0
                                    circuitRound++
                                }
                            }
                            else -> {
                                // Rest between exercises
                                if (unit.circuitRestBetweenExercises > 0) {
                                    circuitRestLabel = "Pause"
                                    circuitRestSeconds = unit.circuitRestBetweenExercises
                                    showCircuitRest = true
                                    scope.launch {
                                        while (circuitRestSeconds > 0) {
                                            kotlinx.coroutines.delay(1000)
                                            circuitRestSeconds--
                                        }
                                        showCircuitRest = false
                                        circuitExerciseIdx++
                                    }
                                } else {
                                    circuitExerciseIdx++
                                }
                            }
                        }
                    }
                )
            } else if (!unit.isCircuit) {
                if (!freeOrder) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (currentIndex > 0) {
                                PDButton("Zurück", onClick = { workoutVm.previousExercise() },
                                    modifier = Modifier.weight(1f), variant = ButtonVariant.SECONDARY)
                            }
                            if (currentIndex < exercises.size - 1) {
                                PDButton("Nächste Übung", onClick = { workoutVm.nextExercise() },
                                    modifier = Modifier.weight(1f))
                            } else {
                                PDButton("Fertig", onClick = {
                                    if (unit.allowShareWorkout) showFinishOptions = true
                                    else workoutVm.finishSession(false) { navController.navigate("home") { popUpTo(0) } }
                                }, modifier = Modifier.weight(1f))
                            }
                        }
                        Text(
                            "${currentIndex + 1} / ${exercises.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                } else {
                    PDButton(
                        "Fertig", modifier = Modifier.fillMaxWidth().padding(16.dp),
                        onClick = {
                            if (unit.allowShareWorkout) showFinishOptions = true
                            else workoutVm.finishSession(false) { navController.navigate("home") { popUpTo(0) } }
                        }
                    )
                }
            }
        }
    ) { padding ->
        if (unit.isCircuit && showCircuitRest) {
            // Circuit rest screen
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(circuitRestLabel, style = MaterialTheme.typography.titleLarge)
                    Text("$circuitRestSeconds s", style = MaterialTheme.typography.displayLarge,
                        color = TealPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        } else if (unit.isCircuit) {
            // Circuit mode: one exercise at a time, track round
            val ex = exercises.getOrNull(circuitExerciseIdx) ?: return@Scaffold
            val activeEx = resolveActiveExercise(ex, exercisePrefs)
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                // Circuit header
                Surface(color = TealPrimary.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.roundOf.format(circuitRound, unit.circuitRounds), style = MaterialTheme.typography.titleMedium, color = TealPrimary)
                        Text(s.exerciseOf.format(circuitExerciseIdx + 1, exercises.size), style = MaterialTheme.typography.titleMedium)
                    }
                }
                ExerciseView(
                    exercise = activeEx,
                    sessionEntry = session!!.exerciseEntries.getOrNull(circuitExerciseIdx),
                    lastWeight = lastWeights[ex.id],
                    exercisePrefs = exercisePrefs,
                    onSetUpdate = { setIdx, weight, reps, duration ->
                        workoutVm.updateSetEntry(circuitExerciseIdx, setIdx, weight, reps, duration)
                    },
                    onSwapExercise = { idx ->
                        workoutVm.setExercisePreference(ex.id, idx)
                    },
                    onStartGame = if (activeEx.exerciseType != "standard") {
                        { navController.navigate(Screen.ReactionGame.createRoute(
                            exerciseType = activeEx.exerciseType,
                            exerciseId = activeEx.templateId.ifEmpty { activeEx.id },
                            roundSeconds = activeEx.gameParams["roundSeconds"] ?: 60,
                            rounds = activeEx.sets.coerceAtLeast(1),
                            param2 = when (activeEx.exerciseType) {
                                "circle_overlap" -> activeEx.gameParams["circleCount"] ?: 5
                                "color_reaction" -> activeEx.gameParams["avgIntervalSeconds"] ?: 3
                                else -> 0
                            },
                            param3 = when (activeEx.exerciseType) {
                                "circle_overlap" -> activeEx.gameParams["overlapRequired"] ?: 2
                                "color_reaction" -> activeEx.gameParams["colorCount"] ?: 3
                                else -> 0
                            }
                        )) }
                    } else null
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                if (!freeOrder) {
                    val ex = exercises.getOrNull(currentIndex) ?: return@Column
                    val activeEx = resolveActiveExercise(ex, exercisePrefs)
                    ExerciseView(
                        exercise = activeEx,
                        sessionEntry = session!!.exerciseEntries.getOrNull(currentIndex),
                        lastWeight = lastWeights[ex.id],
                        exercisePrefs = exercisePrefs,
                        onSetUpdate = { setIdx, weight, reps, duration ->
                            workoutVm.updateSetEntry(currentIndex, setIdx, weight, reps, duration)
                        },
                        onSwapExercise = { idx -> workoutVm.setExercisePreference(ex.id, idx) },
                        onStartGame = if (activeEx.exerciseType != "standard") {
                            { navController.navigate(Screen.ReactionGame.createRoute(
                                exerciseType = activeEx.exerciseType,
                                exerciseId = activeEx.templateId.ifEmpty { activeEx.id },
                                roundSeconds = activeEx.gameParams["roundSeconds"] ?: 60,
                                rounds = activeEx.sets.coerceAtLeast(1),
                                param2 = when (activeEx.exerciseType) {
                                    "circle_overlap" -> activeEx.gameParams["circleCount"] ?: 5
                                    "color_reaction" -> activeEx.gameParams["avgIntervalSeconds"] ?: 3
                                    else -> 0
                                },
                                param3 = when (activeEx.exerciseType) {
                                    "circle_overlap" -> activeEx.gameParams["overlapRequired"] ?: 2
                                    "color_reaction" -> activeEx.gameParams["colorCount"] ?: 3
                                    else -> 0
                                }
                            )) }
                        } else null
                    )
                } else {
                    exercises.forEachIndexed { idx, ex ->
                        val activeEx = resolveActiveExercise(ex, exercisePrefs)
                        ExerciseView(
                            exercise = activeEx,
                            sessionEntry = session!!.exerciseEntries.getOrNull(idx),
                            lastWeight = lastWeights[ex.id],
                            exercisePrefs = exercisePrefs,
                            onSetUpdate = { setIdx, weight, reps, duration ->
                                workoutVm.updateSetEntry(idx, setIdx, weight, reps, duration)
                            },
                            onSwapExercise = { altIdx -> workoutVm.setExercisePreference(ex.id, altIdx) },
                            onStartGame = if (activeEx.exerciseType != "standard") {
                                { navController.navigate(Screen.ReactionGame.createRoute(
                                    exerciseType = activeEx.exerciseType,
                                    exerciseId = activeEx.templateId.ifEmpty { activeEx.id },
                                    roundSeconds = activeEx.gameParams["roundSeconds"] ?: 60,
                                    rounds = activeEx.sets.coerceAtLeast(1),
                                    param2 = when (activeEx.exerciseType) {
                                        "circle_overlap" -> activeEx.gameParams["circleCount"] ?: 5
                                        "color_reaction" -> activeEx.gameParams["avgIntervalSeconds"] ?: 3
                                        else -> 0
                                    },
                                    param3 = when (activeEx.exerciseType) {
                                        "circle_overlap" -> activeEx.gameParams["overlapRequired"] ?: 2
                                        "color_reaction" -> activeEx.gameParams["colorCount"] ?: 3
                                        else -> 0
                                    }
                                )) }
                            } else null
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp))
                    }
                }
            }
        }
    }

    if (loading) PDLoadingOverlay()
}

@Composable
private fun GameExerciseView(exercise: PlannedExercise, onStartGame: () -> Unit) {
    val gameTitle = when (exercise.exerciseType) {
        "reaction_tap" -> "Schnell Antippen"
        "circle_overlap" -> "Kreise treffen"
        "color_reaction" -> "Reaktion auf Farben"
        else -> exercise.customName
    }
    val gameIcon = when (exercise.exerciseType) {
        "reaction_tap" -> Icons.Default.TouchApp
        "circle_overlap" -> Icons.Default.RadioButtonChecked
        "color_reaction" -> Icons.Default.Palette
        else -> Icons.Default.SportsScore
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = TealPrimary.copy(alpha = 0.15f),
            modifier = Modifier.size(96.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(gameIcon, null, modifier = Modifier.size(52.dp), tint = TealPrimary)
            }
        }
        Text(
            gameTitle,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        val roundSeconds = exercise.gameParams["roundSeconds"] ?: 60
        val rounds = exercise.sets.coerceAtLeast(1)
        Text(
            "$rounds Runde${if (rounds != 1) "n" else ""} × ${roundSeconds}s",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (exercise.customDescription.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    exercise.customDescription,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        PDButton(
            text = "Spiel starten",
            onClick = onStartGame,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun resolveActiveExercise(ex: PlannedExercise, prefs: Map<String, Int>): PlannedExercise {
    val activeIdx = prefs[ex.id] ?: -1
    if (activeIdx < 0 || activeIdx >= ex.alternativeExercises.size) return ex
    val alt = ex.alternativeExercises[activeIdx]
    // Return main exercise data but with alternative's name/description/media
    return ex.copy(
        customName = alt.customName.ifEmpty { ex.customName },
        customDescription = alt.customDescription.ifEmpty { ex.customDescription },
        customPhotoUrls = alt.customPhotoUrls.ifEmpty { ex.customPhotoUrls }
    )
}

@Composable
fun ExerciseView(
    exercise: PlannedExercise,
    sessionEntry: ExerciseEntry?,
    lastWeight: Float?,
    exercisePrefs: Map<String, Int> = emptyMap(),
    onSetUpdate: (Int, Float?, Int?, Int?) -> Unit,
    onSwapExercise: ((Int) -> Unit)? = null,
    onStartGame: (() -> Unit)? = null
) {
    val s = LocalAppStrings.current
    if (exercise.exerciseType != "standard" && onStartGame != null) {
        GameExerciseView(exercise = exercise, onStartGame = onStartGame)
        return
    }
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Image
        if (exercise.customPhotoUrls.isNotEmpty()) {
            AsyncImage(
                model = exercise.customPhotoUrls.firstOrNull(),
                contentDescription = exercise.customName,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(exercise.customName, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (onSwapExercise != null && exercise.alternativeExercises.isNotEmpty()) {
                var showAlts by remember { mutableStateOf(false) }
                TextButton(onClick = { showAlts = true }) {
                    Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(s.alternative)
                }
                if (showAlts) {
                    val activeIdx = exercisePrefs[exercise.id] ?: -1
                    AlertDialog(
                        onDismissRequest = { showAlts = false },
                        title = { Text(s.chooseExerciseTitle) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Original
                                AlternativeRow(
                                    name = exercise.customName,
                                    selected = activeIdx == -1,
                                    onClick = { onSwapExercise(-1); showAlts = false }
                                )
                                exercise.alternativeExercises.forEachIndexed { idx, alt ->
                                    AlternativeRow(
                                        name = alt.customName,
                                        selected = activeIdx == idx,
                                        onClick = { onSwapExercise(idx); showAlts = false }
                                    )
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showAlts = false }) { Text(s.cancel) } },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        if (exercise.customDescription.isNotEmpty()) {
            Text(exercise.customDescription, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        lastWeight?.let { w ->
            Surface(shape = RoundedCornerShape(10.dp), color = TealPrimary.copy(alpha = 0.15f)) {
                Text(
                    "Letztes Gewicht: $w kg",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = TealPrimary
                )
            }
        }

        // Sets
        Text(s.setsLabel, style = MaterialTheme.typography.titleMedium)

        sessionEntry?.sets?.forEachIndexed { setIdx, set ->
            SetRow(
                setNumber = set.setNumber,
                targetReps = exercise.reps,
                targetDuration = exercise.durationSeconds,
                currentWeight = set.weight,
                currentReps = set.reps,
                currentDuration = set.durationSeconds,
                trackWeight = exercise.trackWeight,
                trackReps = exercise.trackReps,
                completed = set.completed,
                onWeightChange = { onSetUpdate(setIdx, it, null, null) },
                onRepsChange = { onSetUpdate(setIdx, null, it, null) },
                onDurationChange = { onSetUpdate(setIdx, null, null, it) },
                onComplete = { onSetUpdate(setIdx, null, null, null) }
            )
        }
    }
}

@Composable
fun AlternativeRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
        color = if (selected) TealPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) Icon(Icons.Default.CheckCircle, null, tint = TealPrimary, modifier = Modifier.size(20.dp))
            else Icon(Icons.Default.RadioButtonUnchecked, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(name, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun SetRow(
    setNumber: Int,
    targetReps: Int,
    targetDuration: Int,
    currentWeight: Float,
    currentReps: Int,
    currentDuration: Int,
    trackWeight: Boolean,
    trackReps: Boolean,
    completed: Boolean,
    onWeightChange: (Float) -> Unit,
    onRepsChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onComplete: () -> Unit
) {
    var timerRunning by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(targetDuration) }
    val scope = rememberCoroutineScope()
    val s = LocalAppStrings.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (completed) TealPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("$setNumber", fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))

            if (targetDuration > 0) {
                // Timer button
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (timerRunning) MaterialTheme.colorScheme.error else TealPrimary)
                        .clickable {
                            if (!timerRunning) {
                                timerRunning = true
                                timerSeconds = targetDuration
                                scope.launch {
                                    while (timerSeconds > 0 && timerRunning) {
                                        delay(1000)
                                        timerSeconds--
                                    }
                                    timerRunning = false
                                    onComplete()
                                }
                            } else {
                                timerRunning = false
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (timerRunning) {
                        Text("$timerSeconds", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text("${targetDuration}s", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            } else if (trackReps) {
                OutlinedTextField(
                    value = if (currentReps == 0) "" else currentReps.toString(),
                    onValueChange = { onRepsChange(it.toIntOrNull() ?: 0) },
                    label = { Text(s.repsLabel) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp)
                )
            } else {
                Text("${targetReps} Wdh.", modifier = Modifier.weight(1f))
            }

            if (trackWeight) {
                OutlinedTextField(
                    value = if (currentWeight == 0f) "" else currentWeight.toString(),
                    onValueChange = { onWeightChange(it.toFloatOrNull() ?: 0f) },
                    label = { Text("kg") },
                    modifier = Modifier.width(90.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Checkbox(
                checked = completed,
                onCheckedChange = { if (it) onComplete() },
                colors = CheckboxDefaults.colors(checkedColor = TealPrimary)
            )
        }
    }
}

@Composable
fun FinishDialog(onShare: () -> Unit, onNoShare: () -> Unit) {
    val s = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onNoShare,
        title = { Text(s.workoutCompleted) },
        text = { Text(s.shareWorkoutQuestion) },
        confirmButton = {
            Button(onClick = onShare) { Text(s.shareWorkout) }
        },
        dismissButton = {
            TextButton(onClick = onNoShare) { Text(s.dontShare) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

