package com.practicedyad.app.ui.screens.progress

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.ProgressEntry
import com.practicedyad.app.data.model.ProgressPeriod
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.ui.theme.chartColors
import com.practicedyad.app.viewmodel.ProgressViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    navController: NavController,
    viewAthleteId: String = "",
    vm: ProgressViewModel = hiltViewModel()
) {
    val entries by vm.filteredEntries.collectAsStateWithLifecycle()
    val period by vm.period.collectAsStateWithLifecycle()
    val selectedExercises by vm.selectedExercises.collectAsStateWithLifecycle()
    val exerciseNames by vm.exerciseNames.collectAsStateWithLifecycle()

    LaunchedEffect(viewAthleteId) { vm.load(viewAthleteId) }

    Scaffold(
        topBar = {
            PDTopBar(
                title = if (viewAthleteId.isEmpty()) "Trainingsfortschritt" else "Fortschritt",
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Period selector
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    ProgressPeriod.ONE_MONTH to "1M",
                    ProgressPeriod.SIX_MONTHS to "6M",
                    ProgressPeriod.ONE_YEAR to "1J",
                    ProgressPeriod.ALL to "Alle"
                ).forEach { (p, label) ->
                    PDChip(text = label, selected = period == p, onClick = { vm.setPeriod(p) })
                }
            }

            Spacer(Modifier.height(16.dp))

            // Chart
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(250.dp).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Noch keine Daten vorhanden.\nAbsolviere Workouts, um deinen Fortschritt zu sehen.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Group by exerciseId
                val grouped = entries.groupBy { it.exerciseId }
                val displayedIds = if (selectedExercises.isEmpty()) grouped.keys else selectedExercises.intersect(grouped.keys)

                ProgressChart(
                    grouped = grouped.filterKeys { it in displayedIds },
                    exerciseNames = exerciseNames,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Exercise selector
            PDSectionHeader(
                text = "Übungen",
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // "Alle" chip
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PDChip(
                        text = "Alle",
                        selected = selectedExercises.isEmpty(),
                        onClick = { vm.clearSelection() }
                    )
                }
                // Exercise chips
                exerciseNames.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { (id, name) ->
                            PDChip(
                                text = name.take(12),
                                selected = id in selectedExercises,
                                onClick = { vm.toggleExercise(id) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Legend
                val grouped = entries.groupBy { it.exerciseId }
                val displayedIds = if (selectedExercises.isEmpty()) grouped.keys else selectedExercises.intersect(grouped.keys)
                displayedIds.forEachIndexed { idx, id ->
                    val color = chartColors[idx % chartColors.size]
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(12.dp, 4.dp).background(color, RoundedCornerShape(2.dp)))
                        Text(exerciseNames[id] ?: id, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Reaction game results
            val reactionEntries = entries.filter { it.avgReactionMs > 0 || it.correctAttempts + it.wrongAttempts > 0 }
            if (reactionEntries.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                PDSectionHeader(text = "Reaktionsspiele")
                ReactionProgressSection(
                    entries = reactionEntries,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProgressChart(
    grouped: Map<String, List<ProgressEntry>>,
    exerciseNames: Map<String, String>,
    modifier: Modifier = Modifier
) {
    if (grouped.isEmpty()) return

    val colors = chartColors
    val fmt = SimpleDateFormat("dd.MM", Locale.GERMAN)

    // Collect all dates for X axis
    val allDates = grouped.values.flatten().map { it.date.toDate() }.sortedBy { it.time }
    val minTime = allDates.firstOrNull()?.time ?: return
    val maxTime = allDates.lastOrNull()?.time ?: return
    val timeRange = (maxTime - minTime).coerceAtLeast(1)

    // Max value for Y axis
    val maxValue = grouped.values.flatten().maxOfOrNull { it.maxWeight.coerceAtLeast(it.totalReps.toFloat()) } ?: 1f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        val chartW = size.width - 40f
        val chartH = size.height - 32f
        val left = 40f
        val top = 8f

        // Grid lines
        repeat(4) { i ->
            val y = top + chartH * (1 - i / 3f)
            drawLine(Color.Gray.copy(alpha = 0.2f), Offset(left, y), Offset(left + chartW, y), 1f)
        }

        grouped.entries.forEachIndexed { colorIdx, (id, entries) ->
            val color = colors[colorIdx % colors.size]
            val sortedEntries = entries.sortedBy { it.date.seconds }

            val points = sortedEntries.map { entry ->
                val x = if (timeRange == 0L) left + chartW / 2
                        else left + chartW * ((entry.date.toDate().time - minTime).toFloat() / timeRange)
                val value = entry.maxWeight.coerceAtLeast(0f)
                val y = top + chartH * (1f - value / maxValue.coerceAtLeast(1f))
                Offset(x, y)
            }

            if (points.size >= 2) {
                val path = Path()
                path.moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { path.lineTo(it.x, it.y) }
                drawPath(path, color, style = Stroke(width = 3f))
            }

            points.forEach { p ->
                drawCircle(color, radius = 5f, center = p)
            }
        }
    }

    // X axis labels
    if (allDates.size >= 2) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(fmt.format(allDates.first()), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 40.dp))
            Text(fmt.format(allDates.last()), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReactionProgressSection(
    entries: List<ProgressEntry>,
    modifier: Modifier = Modifier
) {
    val fmt = SimpleDateFormat("dd.MM", Locale.GERMAN)
    // Group by exercise name
    val byExercise = entries.groupBy { it.exerciseName }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        byExercise.entries.forEachIndexed { idx, (name, exEntries) ->
            val sorted = exEntries.sortedBy { it.date.seconds }
            val hasReactionTime = sorted.any { it.avgReactionMs > 0 }
            val hasAccuracy = sorted.any { it.correctAttempts + it.wrongAttempts > 0 }
            val color = chartColors[idx % chartColors.size]

            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))

                    // Latest values summary
                    val latest = sorted.last()
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (hasReactionTime && latest.avgReactionMs > 0) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${latest.avgReactionMs} ms",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                                Text("Ø Reaktionszeit", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (hasAccuracy && latest.correctAttempts + latest.wrongAttempts > 0) {
                            val total = latest.correctAttempts + latest.wrongAttempts
                            val rate = (latest.correctAttempts * 100f / total).roundToInt()
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$rate %",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TealPrimary
                                )
                                Text("Erfolgsquote", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${latest.correctAttempts}/${total}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text("Richtig/Gesamt", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Mini-chart if multiple data points
                    if (sorted.size >= 2) {
                        Spacer(Modifier.height(12.dp))

                        if (hasReactionTime) {
                            val values = sorted.map { it.avgReactionMs.toFloat() }
                            ReactionMiniChart(
                                values = values,
                                dates = sorted.map { fmt.format(it.date.toDate()) },
                                color = color,
                                label = "Reaktionszeit (ms)",
                                invertY = true  // lower = better
                            )
                        }
                        if (hasAccuracy) {
                            val values = sorted.map { e ->
                                val t = e.correctAttempts + e.wrongAttempts
                                if (t == 0) 0f else e.correctAttempts * 100f / t
                            }
                            ReactionMiniChart(
                                values = values,
                                dates = sorted.map { fmt.format(it.date.toDate()) },
                                color = color,
                                label = "Erfolgsquote (%)",
                                invertY = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReactionMiniChart(
    values: List<Float>,
    dates: List<String>,
    color: Color,
    label: String,
    invertY: Boolean
) {
    Text(label, style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(1f)
        val w = size.width
        val h = size.height

        val points = values.mapIndexed { i, v ->
            val x = if (values.size == 1) w / 2f else w * i / (values.size - 1).toFloat()
            val norm = (v - min) / range
            val y = if (invertY) h * norm else h * (1f - norm)
            Offset(x, y)
        }

        if (points.size >= 2) {
            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { path.lineTo(it.x, it.y) }
            drawPath(path, color, style = Stroke(width = 2.5f))
        }
        points.forEach { drawCircle(color, radius = 4f, center = it) }
    }

    // First/last date labels
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(dates.first(), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(dates.last(), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
