package com.practicedyad.app.ui.screens.trainingplans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.data.model.*
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.theme.LocalAppLanguage
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.ui.theme.localDescription
import com.practicedyad.app.ui.theme.localName
import com.practicedyad.app.ui.theme.translateCategory
import com.practicedyad.app.viewmodel.ExerciseViewModel
import com.practicedyad.app.viewmodel.TrainingPlanViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTrainingPlanScreen(
    navController: NavController,
    editPlanId: String,
    planVm: TrainingPlanViewModel = hiltViewModel(),
    exVm: ExerciseViewModel = hiltViewModel()
) {
    val plans by planVm.plans.collectAsStateWithLifecycle()
    val existingPlan = plans.find { it.id == editPlanId }

    var planName by remember { mutableStateOf(existingPlan?.name ?: "") }
    var planDescription by remember { mutableStateOf(existingPlan?.description ?: "") }
    var workoutUnits by remember {
        mutableStateOf(existingPlan?.workoutUnits ?: emptyList())
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editingUnitIndex by remember { mutableStateOf<Int?>(null) }

    val standardExercises by exVm.standardExercises.collectAsStateWithLifecycle()
    val customExercises by exVm.customExercises.collectAsStateWithLifecycle()

    val s = LocalAppStrings.current

    LaunchedEffect(Unit) {
        planVm.loadCoachPlans()
        exVm.loadExercises()
    }

    Scaffold(
        topBar = {
            PDTopBar(
                title = if (editPlanId.isEmpty()) s.newTrainingPlan else s.editPlan,
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                if (editPlanId.isNotEmpty()) {
                    PDButton(
                        text = "Trainingsplan löschen",
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.DANGER
                    )
                    Spacer(Modifier.height(8.dp))
                }
                PDButton(
                    text = "Fertig",
                    onClick = {
                        val plan = TrainingPlan(
                            id = editPlanId.ifEmpty { UUID.randomUUID().toString() },
                            coachId = exVm.currentUserId,
                            name = planName,
                            description = planDescription,
                            workoutUnits = workoutUnits
                        )
                        planVm.savePlan(plan) { navController.popBackStack() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = planName.isNotBlank()
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PDTextField(value = planName, onValueChange = { planName = it },
                    label = "Trainingsplanname")
            }

            item {
                PDTextField(
                    value = planDescription,
                    onValueChange = { planDescription = it },
                    label = "Beschreibung (optional)",
                    singleLine = false,
                    maxLines = 4
                )
            }

            item { PDSectionHeader("Schwerpunkte") }

            itemsIndexed(workoutUnits) { index, unit ->
                WorkoutUnitEditor(
                    unit = unit,
                    allExercises = standardExercises + customExercises,
                    navController = navController,
                    onUpdate = { updated ->
                        workoutUnits = workoutUnits.toMutableList().also { it[index] = updated }
                    },
                    onDelete = {
                        workoutUnits = workoutUnits.toMutableList().also { it.removeAt(index) }
                    }
                )
            }

            item {
                OutlinedButton(
                    onClick = {
                        workoutUnits = workoutUnits + WorkoutUnit(
                            id = UUID.randomUUID().toString(),
                            name = "Neuer Schwerpunkt"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(s.addFocus)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showDeleteConfirm) {
        PDConfirmDialog(
            title = s.deletePlan,
            message = s.deletePlanBody,
            confirmText = s.delete,
            onConfirm = {
                planVm.deletePlan(editPlanId)
                navController.popBackStack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutUnitEditor(
    unit: WorkoutUnit,
    allExercises: List<ExerciseTemplate>,
    navController: NavController,
    onUpdate: (WorkoutUnit) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    var showExercisePicker by remember { mutableStateOf(false) }
    val lang = LocalAppLanguage.current
    val s = LocalAppStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Unit name + expand toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = TealPrimary)
                }
                OutlinedTextField(
                    value = unit.name,
                    onValueChange = { onUpdate(unit.copy(name = it)) },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.titleMedium,
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    label = { Text("Name des Schwerpunkts") }
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
                }
            }

            AnimatedExpanded(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(8.dp))

                    // Schedule Settings
                    ScheduleSection(unit = unit, onUpdate = onUpdate)

                    PDDivider()

                    PDDivider()

                    // Settings toggles
                    PDToggle(
                        checked = unit.athleteChoosesOrder,
                        onCheckedChange = { onUpdate(unit.copy(athleteChoosesOrder = it)) },
                        label = "Athlet*innen wählen Übungsreihenfolge"
                    )
                    TooltipToggle(
                        checked = unit.allowShareWorkout,
                        onCheckedChange = { onUpdate(unit.copy(allowShareWorkout = it)) },
                        label = "Workout teilen freischalten",
                        tooltip = "Wenn aktiviert, können Athlet*innen nach dem Workout-Abschluss ihre Ergebnisse mit dem Coach teilen."
                    )
                    TooltipToggle(
                        checked = unit.allowShareProgress,
                        onCheckedChange = { onUpdate(unit.copy(allowShareProgress = it)) },
                        label = "Fortschritt teilen freischalten",
                        tooltip = "Wenn aktiviert, können Athlet*innen ihren Trainingsfortschritt (Gewichte, Wiederholungen) mit dem Coach teilen."
                    )

                    PDDivider()

                    // Exercises
                    Text(s.exercises, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    unit.exercises.forEachIndexed { idx, ex ->
                        PlannedExerciseEditor(
                            exercise = ex,
                            allExercises = allExercises,
                            onUpdate = { updated ->
                                onUpdate(unit.copy(exercises = unit.exercises.toMutableList().also { it[idx] = updated }))
                            },
                            onDelete = {
                                onUpdate(unit.copy(exercises = unit.exercises.toMutableList().also { it.removeAt(idx) }))
                            },
                            onMoveUp = {
                                if (idx > 0) {
                                    val list = unit.exercises.toMutableList()
                                    val tmp = list[idx]; list[idx] = list[idx-1]; list[idx-1] = tmp
                                    onUpdate(unit.copy(exercises = list))
                                }
                            },
                            onMoveDown = {
                                if (idx < unit.exercises.size - 1) {
                                    val list = unit.exercises.toMutableList()
                                    val tmp = list[idx]; list[idx] = list[idx+1]; list[idx+1] = tmp
                                    onUpdate(unit.copy(exercises = list))
                                }
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = { showExercisePicker = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, null, tint = TealPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(s.addExercise)
                    }

                    PDDivider()

                    // Circuit section
                    CircuitSection(unit = unit, onUpdate = onUpdate)
                }
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            exercises = allExercises,
            onSelect = { template ->
                val defaultGameParams = when (template.exerciseType) {
                    "reaction_tap" -> mapOf("roundSeconds" to 60)
                    "circle_overlap" -> mapOf("roundSeconds" to 60, "circleCount" to 5, "overlapRequired" to 2)
                    "color_reaction" -> mapOf("roundSeconds" to 60, "avgIntervalSeconds" to 3, "colorCount" to 3)
                    else -> emptyMap()
                }
                val newEx = PlannedExercise(
                    id = UUID.randomUUID().toString(),
                    templateId = template.id,
                    customName = template.localName(lang),
                    customDescription = template.localDescription(lang),
                    customPhotoUrls = template.photoUrls,
                    orderIndex = unit.exercises.size,
                    exerciseType = template.exerciseType,
                    gameParams = defaultGameParams,
                    sets = if (template.exerciseType != "standard") 3 else 3,
                    ratingItems = template.ratingItems,
                    ratingScale = template.ratingScale
                )
                onUpdate(unit.copy(exercises = unit.exercises + newEx))
                showExercisePicker = false
            },
            onCreateNew = {
                showExercisePicker = false
                navController.navigate(Screen.ExerciseEditor.createRoute())
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleSection(unit: WorkoutUnit, onUpdate: (WorkoutUnit) -> Unit) {
    var scheduleMode by remember {
        mutableStateOf(
            when {
                unit.athleteChoosesDay -> "free"
                unit.scheduledWeekdays.isNotEmpty() -> "weekdays"
                unit.rhythmDays > 0 -> "rhythm"
                else -> "free"
            }
        )
    }
    val s = LocalAppStrings.current
    val weekdays = s.weekdayNames

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (s.exercises == "Exercises") "Training Days" else "Trainingstage", style = MaterialTheme.typography.titleMedium)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val scheduleLabels = if (s.exercises == "Exercises")
            listOf("free" to "Free", "weekdays" to "Weekdays", "rhythm" to "Rhythm")
        else
            listOf("free" to "Frei", "weekdays" to "Wochentage", "rhythm" to "Rhythmus")
        scheduleLabels.forEach { (mode, label) ->
            PDChip(
                text = label,
                selected = scheduleMode == mode,
                onClick = {
                    scheduleMode = mode
                    onUpdate(unit.copy(
                        athleteChoosesDay = mode == "free",
                        scheduledWeekdays = if (mode != "weekdays") emptyList() else unit.scheduledWeekdays,
                        rhythmDays = if (mode != "rhythm") 0 else unit.rhythmDays
                    ))
                }
            )
        }
        InfoIcon("Athlet*innen können ihre Workout-Tage frei wählen, wenn der Modus \"Frei\" ausgewählt ist.")
    }

    when (scheduleMode) {
        "weekdays" -> {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                weekdays.forEachIndexed { idx, name ->
                    val dayNum = idx + 1
                    PDChip(
                        text = name,
                        selected = dayNum in unit.scheduledWeekdays,
                        onClick = {
                            val newDays = if (dayNum in unit.scheduledWeekdays)
                                unit.scheduledWeekdays - dayNum else unit.scheduledWeekdays + dayNum
                            onUpdate(unit.copy(scheduledWeekdays = newDays))
                        }
                    )
                }
            }
        }
        "rhythm" -> {
            var rhythmText by remember { mutableStateOf(if (unit.rhythmDays > 0) unit.rhythmDays.toString() else "") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (s.exercises == "Exercises") "Every" else "Alle")
                OutlinedTextField(
                    value = rhythmText,
                    onValueChange = {
                        rhythmText = it
                        val days = it.toIntOrNull() ?: 0
                        onUpdate(unit.copy(rhythmDays = days))
                    },
                    modifier = Modifier.width(70.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Text(if (s.exercises == "Exercises") "days" else "Tage")
            }
        }
    }

    // Mindestabstand
    Spacer(Modifier.height(4.dp))
    var minRestText by remember { mutableStateOf(if (unit.minRestDays > 0) unit.minRestDays.toString() else "") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (s.exercises == "Exercises") "Min. rest:" else "Mindestabstand:", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = minRestText,
            onValueChange = {
                minRestText = it
                onUpdate(unit.copy(minRestDays = it.toIntOrNull() ?: 0))
            },
            modifier = Modifier.width(70.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
        )
        Text(if (s.exercises == "Exercises") "days" else "Tage", style = MaterialTheme.typography.bodyMedium)
        InfoIcon("Falls ein Workout nicht am geplanten Tag stattfindet und auf einen anderen Tag verschoben wird, stellt der Mindestabstand sicher, dass das nächste Workout dieses Schwerpunkts erst nach Ablauf der eingestellten Tage gestartet werden kann.")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlannedExerciseEditor(
    exercise: PlannedExercise,
    allExercises: List<ExerciseTemplate> = emptyList(),
    onUpdate: (PlannedExercise) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showAltDbPicker by remember { mutableStateOf(false) }
    val lang = LocalAppLanguage.current
    val s = LocalAppStrings.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        exercise.customName.ifEmpty { "Übung benennen" },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (exercise.exerciseType == "ratings") {
                        Text(
                            "${exercise.ratingItems.size} Items · Skala 1–${exercise.ratingScale}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (exercise.exerciseType in listOf("reflection_journal", "reflection_weekly")) {
                        Text("Reflexion", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (exercise.exerciseType != "standard") {
                        val rs = exercise.gameParams["roundSeconds"] ?: 60
                        Text(
                            "${exercise.sets} Runden × ${rs}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            "${exercise.sets}×${if (exercise.durationSeconds > 0) "${exercise.durationSeconds}s" else "${exercise.reps} Wdh."}  |  Pause: ${exercise.restSeconds}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(18.dp))
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
            }

            AnimatedExpanded(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    PDTextField(value = exercise.customName, onValueChange = { onUpdate(exercise.copy(customName = it)) },
                        label = "Übungsname")
                    PDTextField(
                        value = exercise.customDescription,
                        onValueChange = { onUpdate(exercise.copy(customDescription = it)) },
                        label = "Beschreibung", singleLine = false, maxLines = 3
                    )

                    if (exercise.exerciseType == "ratings") {
                        // Rating items editor
                        var newItemText by remember { mutableStateOf("") }
                        Text("Bewertungs-Items", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            NumberField("Skala (1–N)", exercise.ratingScale.coerceIn(2, 10), Modifier.weight(1f)) {
                                onUpdate(exercise.copy(ratingScale = it.coerceIn(2, 10)))
                            }
                        }
                        exercise.ratingItems.forEachIndexed { i, item ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("• $item", modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = {
                                    onUpdate(exercise.copy(ratingItems = exercise.ratingItems.toMutableList().also { it.removeAt(i) }))
                                }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newItemText,
                                onValueChange = { newItemText = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Neues Item") },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            IconButton(onClick = {
                                if (newItemText.isNotBlank()) {
                                    onUpdate(exercise.copy(ratingItems = exercise.ratingItems + newItemText.trim()))
                                    newItemText = ""
                                }
                            }) {
                                Icon(Icons.Default.Add, null, tint = TealPrimary)
                            }
                        }
                    } else if (exercise.exerciseType in listOf("reflection_journal", "reflection_weekly")) {
                        // Reflection: no sets/params needed, just description
                        Text("Reflexionsübung – Athlet*innen schreiben einen Text, der in den Trainingsnotizen gespeichert wird.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (exercise.exerciseType != "standard") {
                        // Game exercise: show rounds + game-specific params
                        val params = exercise.gameParams.toMutableMap()
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField("Runden", exercise.sets.coerceAtLeast(1), Modifier.weight(1f)) {
                                onUpdate(exercise.copy(sets = it.coerceAtLeast(1)))
                            }
                            NumberField("Rundenzeit(s)", params["roundSeconds"] ?: 60, Modifier.weight(1f)) {
                                onUpdate(exercise.copy(gameParams = params + ("roundSeconds" to it)))
                            }
                        }
                        when (exercise.exerciseType) {
                            "circle_overlap" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField("Kreise", params["circleCount"] ?: 5, Modifier.weight(1f)) {
                                    onUpdate(exercise.copy(gameParams = params + ("circleCount" to it.coerceAtLeast(1))))
                                }
                                NumberField("Überlapp.", params["overlapRequired"] ?: 2, Modifier.weight(1f)) {
                                    onUpdate(exercise.copy(gameParams = params + ("overlapRequired" to it.coerceAtLeast(1))))
                                }
                            }
                            "color_reaction" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField("Intervall(s)", params["avgIntervalSeconds"] ?: 3, Modifier.weight(1f)) {
                                    onUpdate(exercise.copy(gameParams = params + ("avgIntervalSeconds" to it.coerceAtLeast(1))))
                                }
                                NumberField("Farben", params["colorCount"] ?: 3, Modifier.weight(1f)) {
                                    onUpdate(exercise.copy(gameParams = params + ("colorCount" to it.coerceIn(1, 7))))
                                }
                            }
                        }
                    } else {
                        // Standard exercise
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField("Sätze", exercise.sets, Modifier.weight(1f)) { onUpdate(exercise.copy(sets = it)) }
                            if (exercise.durationSeconds > 0) {
                                NumberField("Sekunden", exercise.durationSeconds, Modifier.weight(1f)) {
                                    onUpdate(exercise.copy(durationSeconds = it))
                                }
                            } else {
                                NumberField("Wdh.", exercise.reps, Modifier.weight(1f)) { onUpdate(exercise.copy(reps = it)) }
                            }
                            NumberField("Pause(s)", exercise.restSeconds, Modifier.weight(1f)) {
                                onUpdate(exercise.copy(restSeconds = it))
                            }
                        }

                        PDToggle(
                            checked = exercise.durationSeconds > 0,
                            onCheckedChange = { useDuration ->
                                onUpdate(exercise.copy(
                                    durationSeconds = if (useDuration) 30 else 0,
                                    reps = if (useDuration) 0 else 10
                                ))
                            },
                            label = "Ausführungszeit statt Wiederholungen"
                        )
                        PDToggle(
                            checked = exercise.trackWeight,
                            onCheckedChange = { onUpdate(exercise.copy(trackWeight = it)) },
                            label = "Gewicht tracken"
                        )
                        PDToggle(
                            checked = exercise.trackReps,
                            onCheckedChange = { onUpdate(exercise.copy(trackReps = it)) },
                            label = "Wiederholungen tracken"
                        )
                    }

                    PDDivider()

                    // Alternative exercises
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.alternativeExercises, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (exercise.alternativeExercises.size < 3) {
                            TextButton(onClick = {
                                val newAlts = exercise.alternativeExercises + AlternativeExercise(
                                    id = UUID.randomUUID().toString()
                                )
                                onUpdate(exercise.copy(alternativeExercises = newAlts))
                            }) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Text(s.add)
                            }
                            if (allExercises.isNotEmpty()) {
                                TextButton(onClick = { showAltDbPicker = true }) {
                                    Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp))
                                    Text(s.fromDatabase)
                                }
                            }
                        }
                    }
                    exercise.alternativeExercises.forEachIndexed { altIdx, alt ->
                        Surface(shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Alternative ${altIdx + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = TealPrimary,
                                        modifier = Modifier.weight(1f))
                                    IconButton(onClick = {
                                        val newAlts = exercise.alternativeExercises.toMutableList().also { it.removeAt(altIdx) }
                                        onUpdate(exercise.copy(alternativeExercises = newAlts))
                                    }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Default.Close, null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp))
                                    }
                                }
                                OutlinedTextField(
                                    value = alt.customName,
                                    onValueChange = { v ->
                                        val newAlts = exercise.alternativeExercises.toMutableList()
                                        newAlts[altIdx] = alt.copy(customName = v)
                                        onUpdate(exercise.copy(alternativeExercises = newAlts))
                                    },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = alt.customDescription,
                                    onValueChange = { v ->
                                        val newAlts = exercise.alternativeExercises.toMutableList()
                                        newAlts[altIdx] = alt.copy(customDescription = v)
                                        onUpdate(exercise.copy(alternativeExercises = newAlts))
                                    },
                                    label = { Text("Beschreibung (optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = false, maxLines = 2,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAltDbPicker) {
        ExercisePickerDialog(
            exercises = allExercises,
            onSelect = { template ->
                val newAlts = exercise.alternativeExercises + AlternativeExercise(
                    id = UUID.randomUUID().toString(),
                    templateId = template.id,
                    customName = template.localName(lang),
                    customDescription = template.localDescription(lang),
                    customPhotoUrls = template.photoUrls
                )
                onUpdate(exercise.copy(alternativeExercises = newAlts))
                showAltDbPicker = false
            },
            onCreateNew = { showAltDbPicker = false },
            onDismiss = { showAltDbPicker = false }
        )
    }
}

@Composable
private fun NumberField(label: String, value: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { onChange(it.toIntOrNull() ?: 0) },
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(10.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimatedExpanded(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) { content() }
}

@Composable
private fun InfoIcon(text: String) {
    var show by remember { mutableStateOf(false) }
    IconButton(onClick = { show = true }, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Default.HelpOutline, null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { show = false }) { Text("OK") } },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun TooltipToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    tooltip: String
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        PDToggle(
            checked = checked,
            onCheckedChange = onCheckedChange,
            label = label,
            modifier = Modifier.weight(1f)
        )
        InfoIcon(tooltip)
    }
}

@Composable
fun CircuitSection(unit: WorkoutUnit, onUpdate: (WorkoutUnit) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val circuitExercises = unit.exercises.filter { it.circuitGroupId.isNotEmpty() }
    val hasCircuit = circuitExercises.isNotEmpty()
    val circuitGroupId = remember { UUID.randomUUID().toString() }
    val s = LocalAppStrings.current

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Circuit Training", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (hasCircuit) {
            IconButton(onClick = {
                onUpdate(unit.copy(
                    exercises = unit.exercises.map { it.copy(circuitGroupId = "") },
                    circuit = false
                ))
            }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (hasCircuit) {
        Text(s.exerciseCount.format(circuitExercises.size) + " im Circuit", style = MaterialTheme.typography.bodySmall, color = TealPrimary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Runden", unit.circuitRounds, Modifier.weight(1f)) { onUpdate(unit.copy(circuitRounds = it)) }
            NumberField("Pause Üb. (s)", unit.circuitRestBetweenExercises, Modifier.weight(1f)) { onUpdate(unit.copy(circuitRestBetweenExercises = it)) }
            NumberField("Pause Runde (s)", unit.circuitRestBetweenRounds, Modifier.weight(1f)) { onUpdate(unit.copy(circuitRestBetweenRounds = it)) }
        }
        TextButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.chooseExercise)
        }
    } else {
        if (unit.exercises.isNotEmpty()) {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Loop, null, tint = TealPrimary)
                Spacer(Modifier.width(8.dp))
                Text(s.groupIntoCircuit)
            }
        } else {
            Text(s.addExercisesFirst, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showPicker && unit.exercises.isNotEmpty()) {
        val groupId = unit.exercises.firstOrNull { it.circuitGroupId.isNotEmpty() }?.circuitGroupId ?: circuitGroupId
        val selected = remember { mutableStateListOf<String>().also { list ->
            list.addAll(unit.exercises.filter { it.circuitGroupId.isNotEmpty() }.map { it.id })
        }}

        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(s.circuitExercisesTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s.circuitExercisesBody, style = MaterialTheme.typography.bodySmall)
                    unit.exercises.forEach { ex ->
                        val isChecked = ex.id in selected
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (isChecked) selected.remove(ex.id) else selected.add(ex.id)
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = {
                                if (it) selected.add(ex.id) else selected.remove(ex.id)
                            })
                            Spacer(Modifier.width(8.dp))
                            Text(ex.customName.ifEmpty { "Übung" })
                        }
                    }
                    OutlinedButton(onClick = {
                        selected.clear()
                        unit.exercises.forEach { selected.add(it.id) }
                    }, modifier = Modifier.fillMaxWidth()) { Text(s.selectAll) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updated = unit.exercises.map { ex ->
                        if (ex.id in selected) ex.copy(circuitGroupId = groupId)
                        else ex.copy(circuitGroupId = "")
                    }
                    onUpdate(unit.copy(exercises = updated, circuit = selected.isNotEmpty()))
                    showPicker = false
                }) { Text(s.apply) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(s.cancel) } },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private fun fuzzyMatch(query: String, exercise: ExerciseTemplate): Boolean {
    if (query.isBlank()) return true
    val q = query.lowercase().trim()
    val texts = buildList {
        add(exercise.nameDE.lowercase())
        if (exercise.nameEN.isNotBlank()) add(exercise.nameEN.lowercase())
        add(exercise.category.lowercase())
        addAll(exercise.categories.map { it.lowercase() })
        addAll(exercise.searchTerms.map { it.lowercase() })
    }
    if (texts.any { it.contains(q) }) return true
    // Simple fuzzy: each word of query must appear partially in some field
    val words = q.split(" ").filter { it.length > 2 }
    return words.all { word -> texts.any { text -> text.contains(word) || levenshteinClose(word, text) } }
}

private fun levenshteinClose(query: String, text: String): Boolean {
    if (query.length < 3) return false
    val words = text.split(" ")
    return words.any { word ->
        if (word.length < 3) return@any false
        val maxDist = when {
            query.length <= 4 -> 1
            query.length <= 7 -> 2
            else -> 3
        }
        levenshtein(query, word.take(query.length + 2)) <= maxDist
    }
}

private fun levenshtein(a: String, b: String): Int {
    val m = a.length; val n = b.length
    val dp = Array(m + 1) { IntArray(n + 1) }
    for (i in 0..m) dp[i][0] = i
    for (j in 0..n) dp[0][j] = j
    for (i in 1..m) for (j in 1..n) {
        dp[i][j] = if (a[i-1] == b[j-1]) dp[i-1][j-1]
        else 1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
    }
    return dp[m][n]
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePickerDialog(
    exercises: List<ExerciseTemplate>,
    onSelect: (ExerciseTemplate) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf("") }
    var filterMaterial by remember { mutableStateOf("") }
    val lang = LocalAppLanguage.current
    val s = LocalAppStrings.current

    val categories = remember(exercises) { exercises.map { it.category }.filter { it.isNotEmpty() }.distinct().sortedBy { categoryOrder(it) } }
    val materials = remember(exercises) { exercises.map { it.material }.filter { it.isNotEmpty() }.distinct().sorted() }

    val filtered = remember(search, filterCategory, filterMaterial, exercises, lang) {
        exercises.filter { ex ->
            fuzzyMatch(search, ex) &&
            (filterCategory.isEmpty() || ex.category == filterCategory || filterCategory in ex.categories) &&
            (filterMaterial.isEmpty() || ex.material == filterMaterial)
        }.sortedBy { it.localName(lang) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.chooseExercise) },
        text = {
            Column {
                OutlinedTextField(
                    value = search, onValueChange = { search = it },
                    label = { Text(s.search) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = if (search.isNotEmpty()) { { IconButton(onClick = { search = "" }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp)) } } } else null
                )
                Spacer(Modifier.height(6.dp))

                // Category filter
                Text(s.language.let { "Kategorie" }.let { if (lang == com.practicedyad.app.data.model.AppLanguage.ENGLISH) "Category" else it },
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PDChip(s.catAll, filterCategory.isEmpty()) { filterCategory = "" }
                    categories.forEach { cat ->
                        PDChip(s.translateCategory(cat), filterCategory == cat) {
                            filterCategory = if (filterCategory == cat) "" else cat
                        }
                    }
                }

                // Material filter
                Text(if (lang == com.practicedyad.app.data.model.AppLanguage.ENGLISH) "Equipment" else "Hilfsmittel",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PDChip(s.matAll, filterMaterial.isEmpty()) { filterMaterial = "" }
                    materials.forEach { mat ->
                        PDChip(s.translateCategory(mat), filterMaterial == mat) {
                            filterMaterial = if (filterMaterial == mat) "" else mat
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("${filtered.size} Übungen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))

                Column(
                    modifier = Modifier.height(260.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    filtered.forEach { ex ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(ex) },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(ex.localName(lang), fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (ex.category.isNotEmpty()) {
                                        Text(ex.category, style = MaterialTheme.typography.bodySmall,
                                            color = TealPrimary)
                                    }
                                    if (ex.material.isNotEmpty()) {
                                        Text("· ${ex.material}", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(s.noExercisesFound, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateNew) { Text(s.createNewExercise) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(s.cancel) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
