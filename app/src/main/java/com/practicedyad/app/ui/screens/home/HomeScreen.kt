package com.practicedyad.app.ui.screens.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.ui.theme.LocalAppLanguage
import com.practicedyad.app.data.model.AppLanguage
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.viewmodel.AuthViewModel
import com.practicedyad.app.viewmodel.TrainingPlanViewModel
import com.practicedyad.app.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authVm: AuthViewModel = hiltViewModel(),
    planVm: TrainingPlanViewModel = hiltViewModel(),
    workoutVm: WorkoutViewModel = hiltViewModel()
) {
    val user by authVm.currentUser.collectAsStateWithLifecycle()
    val plans by planVm.plans.collectAsStateWithLifecycle()
    val sessions by workoutVm.sessions.collectAsStateWithLifecycle()
    val s = LocalAppStrings.current
    val lang = LocalAppLanguage.current

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(user) {
        user?.let { u ->
            when {
                u.role == UserRole.COACH || u.role == UserRole.BOTH -> planVm.loadCoachPlans()
                else -> planVm.loadAthletePlans()
            }
            workoutVm.loadSessions(u.id)
        }
    }

    val today = Calendar.getInstance()
    val todayWorkouts = remember(plans, sessions, today) {
        plans.flatMap { plan ->
            plan.workoutUnits
                .filter { unit ->
                    isScheduledToday(unit, today) &&
                    sessions.none { it.workoutUnitId == unit.id && isSameDay(it.date.toDate()) }
                }
                .map { Pair(plan, it) }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideDrawer(
                user = user,
                navController = navController,
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("PracticeDyad", fontWeight = FontWeight.Bold, color = TealPrimary)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, "Menü")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // 7-Day Calendar Strip
                SevenDayCalendar(
                    plans = plans,
                    sessions = sessions,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                Spacer(Modifier.height(24.dp))

                // Date heading
                val locale = if (lang == AppLanguage.ENGLISH) java.util.Locale.ENGLISH else java.util.Locale.GERMAN
                val datePattern = if (lang == AppLanguage.ENGLISH) "EEEE, MMMM d, yyyy" else "EEEE, d. MMMM yyyy"
                val dateText = remember(lang) {
                    SimpleDateFormat(datePattern, locale).format(java.util.Date())
                }
                Text(
                    dateText,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(20.dp))

                // Today's workouts
                if (todayWorkouts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            s.todayNoWorkout,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(s.todayWorkoutTitle, style = MaterialTheme.typography.titleLarge)
                        todayWorkouts.forEach { (plan, unit) ->
                            TodayWorkoutCard(
                                plan = plan,
                                unit = unit,
                                sessions = sessions,
                                canStart = workoutVm.canStartWorkout(unit, sessions),
                                onStart = {
                                    navController.navigate(
                                        Screen.WorkoutExecution.createRoute(unit.id, plan.id)
                                    )
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun TodayWorkoutCard(
    plan: TrainingPlan,
    unit: WorkoutUnit,
    sessions: List<WorkoutSession>,
    canStart: Boolean,
    onStart: () -> Unit
) {
    var showNotToday by remember { mutableStateOf(false) }
    val completed = sessions.any { it.workoutUnitId == unit.id && isSameDay(it.date.toDate()) }
    val s = LocalAppStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (completed) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(plan.name, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(unit.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (completed) {
                Text(s.completedToday, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Button(
                    onClick = {
                        if (canStart) onStart() else showNotToday = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Start", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showNotToday) {
        AlertDialog(
            onDismissRequest = { showNotToday = false },
            title = { Text(s.notScheduledTitle) },
            text = { Text(s.notScheduledBody) },
            confirmButton = {
                Button(onClick = { showNotToday = false; onStart() }) { Text(s.startAnyway) }
            },
            dismissButton = {
                TextButton(onClick = { showNotToday = false }) { Text(s.cancel) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun SevenDayCalendar(
    plans: List<TrainingPlan>,
    sessions: List<WorkoutSession>,
    modifier: Modifier = Modifier
) {
    val days = remember { (0..6).map { offset ->
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
    }}

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(days) { cal ->
            CalendarDayCell(cal = cal, plans = plans)
        }
    }
}

@Composable
fun CalendarDayCell(cal: Calendar, plans: List<TrainingPlan>) {
    val lang = LocalAppLanguage.current
    val locale = if (lang == AppLanguage.ENGLISH) java.util.Locale.ENGLISH else java.util.Locale.GERMAN
    val dayName = SimpleDateFormat("EEE", locale).format(cal.time).replaceFirstChar { it.uppercase() }
    val dayNum = cal.get(Calendar.DAY_OF_MONTH)
    val month = SimpleDateFormat("MMM", locale).format(cal.time)
    val isToday = isSameDay(cal.time)

    val trainingsToday = plans.flatMap { plan ->
        plan.workoutUnits.filter { isScheduledOnDay(it, cal) }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.width(58.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(dayName, style = MaterialTheme.typography.labelSmall,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$dayNum", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            Text(month, style = MaterialTheme.typography.labelSmall,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary.copy(0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)

            if (trainingsToday.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isToday) Color.White.copy(alpha = 0.25f) else TealPrimary
                ) {
                    Text(
                        text = trainingsToday.first().name.split(" ")
                            .map { it.first() }.take(2).joinToString(""),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) Color.White else Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

private fun isSameDay(date: Date): Boolean {
    val cal = Calendar.getInstance()
    val other = Calendar.getInstance().apply { time = date }
    return cal.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
}

private fun isScheduledToday(unit: WorkoutUnit, today: Calendar): Boolean {
    return isScheduledOnDay(unit, today)
}

private fun isScheduledOnDay(unit: WorkoutUnit, cal: Calendar): Boolean {
    if (unit.athleteChoosesDay) return false
    if (unit.scheduledWeekdays.isNotEmpty()) {
        val dow = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1 // 1=Mon..7=Sun
        return dow in unit.scheduledWeekdays
    }
    if (unit.rhythmDays > 0 && unit.startDate != null) {
        val startCal = Calendar.getInstance().apply { time = unit.startDate.toDate() }
        val diffDays = ((cal.timeInMillis - startCal.timeInMillis) / 86400000).toInt()
        return diffDays >= 0 && diffDays % unit.rhythmDays == 0
    }
    return false
}
