package com.practicedyad.app.ui.screens.athletes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.WorkoutSession
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.viewmodel.AuthViewModel
import com.practicedyad.app.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SharedWorkoutsScreen(
    navController: NavController,
    athleteId: String,
    workoutVm: WorkoutViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel()
) {
    val user by authVm.currentUser.collectAsStateWithLifecycle()
    val coachId = user?.id ?: ""
    val sessions by workoutVm.getSharedSessions(coachId, athleteId).collectAsState(emptyList())

    Scaffold(
        topBar = { PDTopBar("Durchgeführte Workouts", onBack = { navController.popBackStack() }) }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("Noch keine geteilten Workouts.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessions) { session ->
                    WorkoutSessionCard(
                        session = session,
                        onClick = { navController.navigate(Screen.SharedWorkoutDetail.createRoute(session.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutSessionCard(session: WorkoutSession, onClick: () -> Unit) {
    val fmt = SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.GERMAN)
    val dateStr = fmt.format(session.date.toDate())

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.workoutUnitName, fontWeight = FontWeight.Bold)
                Text(dateStr, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (session.durationMinutes > 0) {
                    Text("${session.durationMinutes} Min.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SharedWorkoutDetailScreen(
    navController: NavController,
    sessionId: String,
    workoutVm: WorkoutViewModel = hiltViewModel()
) {
    val sharedSessions by workoutVm.sessions.collectAsStateWithLifecycle()
    val session = sharedSessions.find { it.id == sessionId }

    Scaffold(
        topBar = { PDTopBar(session?.workoutUnitName ?: "Workout", onBack = { navController.popBackStack() }) }
    ) { padding ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(session.exerciseEntries) { entry ->
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(entry.exerciseName, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        entry.sets.forEach { set ->
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Satz ${set.setNumber}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall)
                                if (set.weight > 0) Text("${set.weight} kg")
                                if (set.reps > 0) Text("${set.reps} Wdh.")
                                if (set.durationSeconds > 0) Text("${set.durationSeconds}s")
                            }
                        }
                    }
                }
            }
        }
    }
}
