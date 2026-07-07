package com.practicedyad.app.ui.screens.trainingplans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.PlannedExercise
import com.practicedyad.app.data.model.WorkoutUnit
import com.practicedyad.app.ui.components.PDTopBar
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.viewmodel.TrainingPlanViewModel

@Composable
fun TrainingPlanPreviewScreen(
    navController: NavController,
    planId: String,
    planVm: TrainingPlanViewModel = hiltViewModel()
) {
    val plans by planVm.plans.collectAsStateWithLifecycle()
    val plan = plans.find { it.id == planId }
    val s = LocalAppStrings.current

    LaunchedEffect(Unit) { planVm.loadCoachPlans(); planVm.loadAthletePlans() }

    Scaffold(
        topBar = {
            PDTopBar(
                title = plan?.name ?: s.trainingPlans,
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        if (plan == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (plan.description.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(alpha = 0.1f))
                    ) {
                        Text(
                            plan.description,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            items(plan.workoutUnits) { unit ->
                PreviewUnitCard(unit = unit, s = s)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PreviewUnitCard(unit: WorkoutUnit, s: com.practicedyad.app.ui.theme.AppStrings) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        unit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val scheduleText = when {
                        unit.scheduledWeekdays.isNotEmpty() ->
                            unit.scheduledWeekdays.map { s.weekdayNames[it - 1] }.joinToString(", ")
                        unit.rhythmDays > 0 -> s.everyXDays.format(unit.rhythmDays)
                        unit.athleteChoosesDay -> s.freelyChoosable
                        else -> ""
                    }
                    if (scheduleText.isNotEmpty()) {
                        Text(
                            scheduleText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        s.exerciseCount.format(unit.exercises.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TealPrimary
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = TealPrimary
                    )
                }
            }

            if (expanded && unit.exercises.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    unit.exercises.forEach { ex ->
                        PreviewExerciseRow(ex)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewExerciseRow(ex: PlannedExercise) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (ex.exerciseType != "standard") Icons.Default.SportsScore else Icons.Default.FitnessCenter,
                null, tint = TealPrimary, modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(ex.customName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            val detail = when {
                ex.exerciseType != "standard" -> {
                    val rs = ex.gameParams["roundSeconds"] ?: 60
                    "${ex.sets} × ${rs}s"
                }
                ex.durationSeconds > 0 -> "${ex.sets} × ${ex.durationSeconds}s"
                else -> "${ex.sets} × ${ex.reps} Wdh."
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (ex.restSeconds > 0) {
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surface) {
                Text(
                    "${ex.restSeconds}s",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
