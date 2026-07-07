package com.practicedyad.app.ui.screens.trainingplans

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.*
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.utils.PdfExporter
import com.practicedyad.app.viewmodel.AuthViewModel
import com.practicedyad.app.viewmodel.TrainingPlanViewModel
import com.practicedyad.app.viewmodel.WorkoutViewModel

@Composable
fun TrainingPlansScreen(
    navController: NavController,
    authVm: AuthViewModel = hiltViewModel(),
    planVm: TrainingPlanViewModel = hiltViewModel(),
    workoutVm: WorkoutViewModel = hiltViewModel()
) {
    val user by authVm.currentUser.collectAsStateWithLifecycle()
    val plans by planVm.plans.collectAsStateWithLifecycle()
    val athletes by planVm.athletes.collectAsStateWithLifecycle()
    val teams by planVm.teams.collectAsStateWithLifecycle()
    val sessions by workoutVm.sessions.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isCoach = user?.role == UserRole.COACH || user?.role == UserRole.BOTH
    // BOTH-role users can share plans with themselves as an athlete
    val shareableAthletes = if (user?.role == UserRole.BOTH) {
        val selfEntry = user?.let { u ->
            athletes.find { it.id == u.id } ?: u.copy()
        }
        if (selfEntry != null && athletes.none { it.id == selfEntry.id }) athletes + selfEntry
        else athletes
    } else athletes

    LaunchedEffect(user) {
        user?.let { u ->
            if (isCoach) {
                planVm.loadCoachPlans()
                planVm.loadAthletes()
                planVm.loadTeams()
            } else {
                planVm.loadAthletePlans()
            }
            workoutVm.loadSessions(u.id)
        }
    }

    var showAddMenu by remember { mutableStateOf(false) }
    val s = LocalAppStrings.current

    Scaffold(
        topBar = {
            PDTopBar(s.trainingPlans, onBack = { navController.popBackStack() })
        },
        floatingActionButton = {
            if (isCoach) {
                Box {
                    FloatingActionButton(
                        onClick = { showAddMenu = true },
                        containerColor = TealPrimary
                    ) { Icon(Icons.Default.Add, "Neuen Plan", tint = Color.White) }

                    DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(s.newTrainingPlanCreate) },
                            leadingIcon = { Icon(Icons.Default.Add, null) },
                            onClick = {
                                showAddMenu = false
                                navController.navigate(Screen.CreatePlan.createRoute())
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (plans.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    message = if (isCoach) s.noPlansCoach else s.noPlansAthlete,
                    icon = { Icon(Icons.Default.FitnessCenter, null, modifier = Modifier.size(48.dp), tint = TealPrimary) }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(plans) { plan ->
                    TrainingPlanCard(
                        plan = plan,
                        isCoach = isCoach,
                        athletes = shareableAthletes,
                        teams = teams,
                        sessions = sessions,
                        canStartWorkout = { unit -> workoutVm.canStartWorkout(unit, sessions) },
                        onEdit = { navController.navigate(Screen.CreatePlan.createRoute(plan.id)) },
                        onPreview = { navController.navigate(Screen.PlanPreview.createRoute(plan.id)) },
                        onShare = { selAthletes, selTeams ->
                            planVm.sharePlan(plan.id, selAthletes, selTeams)
                        },
                        onEnd = { planVm.endPlan(plan.id) },
                        onStartUnit = { unit ->
                            navController.navigate(Screen.WorkoutExecution.createRoute(unit.id, plan.id))
                        },
                        onExportPdf = {
                            val uri = PdfExporter.exportPlan(context, plan)
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "PDF öffnen"))
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun TrainingPlanCard(
    plan: TrainingPlan,
    isCoach: Boolean,
    athletes: List<AppUser>,
    teams: List<Team>,
    sessions: List<WorkoutSession>,
    canStartWorkout: (WorkoutUnit) -> Boolean,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
    onShare: (List<String>, List<String>) -> Unit,
    onEnd: () -> Unit,
    onStartUnit: (WorkoutUnit) -> Unit,
    onExportPdf: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showEndConfirm by remember { mutableStateOf(false) }
    val s = LocalAppStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Plan header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).clickable { expanded = !expanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = TealPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Row {
                    IconButton(onClick = onPreview) {
                        Icon(Icons.Default.Visibility, "Vorschau", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onExportPdf) {
                        Icon(Icons.Default.PictureAsPdf, "PDF", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isCoach) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, "Bearbeiten", tint = TealPrimary)
                        }
                    }
                }
            }

            // Shared with (coach only)
            if (isCoach && (plan.sharedWithAthleteIds.isNotEmpty() || plan.sharedWithTeamIds.isNotEmpty())) {
                val names = (
                    athletes.filter { it.id in plan.sharedWithAthleteIds }.map { it.name } +
                    teams.filter { it.id in plan.sharedWithTeamIds }.map { it.name }
                ).joinToString(", ")
                Text(
                    "Geteilt mit: $names",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp, top = 4.dp)
                )
            }

            // Expanded units
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    plan.workoutUnits.forEach { unit ->
                        WorkoutUnitRow(
                            unit = unit,
                            canStart = canStartWorkout(unit),
                            sessions = sessions,
                            onStart = { onStartUnit(unit) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }

                    if (isCoach) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PDButton(
                                text = s.sharePlan,
                                onClick = { showShareDialog = true },
                                modifier = Modifier.weight(1f),
                                variant = ButtonVariant.SECONDARY
                            )
                            PDButton(
                                text = s.endPlan,
                                onClick = { showEndConfirm = true },
                                modifier = Modifier.weight(1f),
                                variant = ButtonVariant.DANGER
                            )
                        }
                    }
                }
            }
        }
    }

    if (showShareDialog) {
        SharePlanDialog(
            athletes = athletes,
            teams = teams,
            currentAthleteIds = plan.sharedWithAthleteIds,
            currentTeamIds = plan.sharedWithTeamIds,
            onShare = { a, t -> onShare(a, t); showShareDialog = false },
            onDismiss = { showShareDialog = false }
        )
    }

    if (showEndConfirm) {
        PDConfirmDialog(
            title = s.endPlanTitle,
            message = s.endPlanBody,
            confirmText = s.endPlan,
            onConfirm = { onEnd(); showEndConfirm = false },
            onDismiss = { showEndConfirm = false }
        )
    }
}

@Composable
fun WorkoutUnitRow(
    unit: WorkoutUnit,
    canStart: Boolean,
    sessions: List<WorkoutSession>,
    onStart: () -> Unit
) {
    var showNotScheduled by remember { mutableStateOf(false) }
    val s = LocalAppStrings.current

    val scheduleText = when {
        unit.scheduledWeekdays.isNotEmpty() ->
            unit.scheduledWeekdays.map { s.weekdayNames[it - 1] }.joinToString(", ")
        unit.rhythmDays > 0 -> s.everyXDays.format(unit.rhythmDays)
        unit.athleteChoosesDay -> s.freelyChoosable
        else -> ""
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(unit.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (scheduleText.isNotEmpty()) {
                Text(scheduleText, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(s.exerciseCount.format(unit.exercises.size), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Button(
            onClick = {
                if (canStart) onStart()
                else showNotScheduled = true
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canStart) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            enabled = canStart
        ) {
            Text(
                s.start,
                color = if (canStart) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showNotScheduled) {
        AlertDialog(
            onDismissRequest = { showNotScheduled = false },
            title = { Text(s.workoutNotScheduled) },
            text = { Text(s.workoutNotScheduledBody) },
            confirmButton = {
                Button(onClick = { showNotScheduled = false; onStart() }) { Text(s.startAnyway) }
            },
            dismissButton = {
                TextButton(onClick = { showNotScheduled = false }) { Text(s.cancel) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SharePlanDialog(
    athletes: List<AppUser>,
    teams: List<Team>,
    currentAthleteIds: List<String>,
    currentTeamIds: List<String>,
    onShare: (List<String>, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedAthletes by remember { mutableStateOf(currentAthleteIds.toSet()) }
    var selectedTeams by remember { mutableStateOf(currentTeamIds.toSet()) }
    val s = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.sharePlanTitle) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (teams.isNotEmpty()) {
                    Text("Teams", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    teams.forEach { team ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedTeams = if (team.id in selectedTeams)
                                    selectedTeams - team.id else selectedTeams + team.id
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = team.id in selectedTeams, onCheckedChange = null)
                            Text(team.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (athletes.isNotEmpty()) {
                    Text(s.individualAthletes, style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                    athletes.forEach { athlete ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                selectedAthletes = if (athlete.id in selectedAthletes)
                                    selectedAthletes - athlete.id else selectedAthletes + athlete.id
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = athlete.id in selectedAthletes, onCheckedChange = null)
                            Text(athlete.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onShare(selectedAthletes.toList(), selectedTeams.toList()) }) {
                Text(s.sharePlanTitle)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } },
        shape = RoundedCornerShape(20.dp)
    )
}
