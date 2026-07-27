package com.practicedyad.app.ui.screens.exercises

import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.practicedyad.app.data.model.ExerciseTemplate
import com.practicedyad.app.data.model.categoryOrder
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.LocalAppLanguage
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.ui.theme.localDescription
import com.practicedyad.app.ui.theme.localName
import com.practicedyad.app.ui.theme.translateCategory
import com.practicedyad.app.viewmodel.ExerciseViewModel
import com.practicedyad.app.viewmodel.TrainingNotesViewModel

@Composable
fun MyExercisesScreen(
    navController: NavController,
    vm: ExerciseViewModel = hiltViewModel(),
    notesVm: TrainingNotesViewModel = hiltViewModel()
) {
    val customExercises by vm.filteredCustom.collectAsStateWithLifecycle()
    val standardExercises by vm.filteredStandard.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var reflectionTestExercise by remember { mutableStateOf<ExerciseTemplate?>(null) }
    var ratingsTestExercise by remember { mutableStateOf<ExerciseTemplate?>(null) }

    LaunchedEffect(Unit) { vm.loadExercises() }

    val s = LocalAppStrings.current
    val lang = LocalAppLanguage.current

    Scaffold(
        topBar = { PDTopBar(s.myExercises, onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.ExerciseEditor.createRoute()) },
                containerColor = TealPrimary
            ) { Icon(Icons.Default.Add, null, tint = Color.White) }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.setSearch(it) },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text(s.searchExercise) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text(s.myExercises, modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text(s.standardDatabase, modifier = Modifier.padding(12.dp))
                }
            }

            val exercises = if (selectedTab == 0) customExercises else standardExercises

            if (exercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        if (selectedTab == 0) s.noCustomExercises else s.loadingExercises
                    )
                }
            } else {
                val grouped = exercises
                    .flatMap { ex ->
                        val cats = ex.categories.ifEmpty { listOf(ex.category) }
                        cats.map { cat -> cat to ex }
                    }
                    .sortedWith(compareBy({ categoryOrder(it.first) }, { it.second.localName(lang) }))
                    .groupBy({ it.first }, { it.second })
                    .entries
                    .sortedBy { (cat, _) -> categoryOrder(cat) }
                    .associate { it.toPair() }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var lastParent = ""
                    grouped.forEach { (category, categoryExercises) ->
                        val parent = com.practicedyad.app.data.model.PARENT_CATEGORY[category] ?: category
                        val isKraftSub = parent == "Krafttraining" && category != "Krafttraining"
                        if (isKraftSub && parent != lastParent) {
                            lastParent = parent
                            item {
                                Text(
                                    s.translateCategory(parent),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 2.dp)
                                )
                            }
                        } else if (!isKraftSub) {
                            lastParent = parent
                        }
                        item {
                            Text(
                                s.translateCategory(category),
                                style = if (isKraftSub) MaterialTheme.typography.titleSmall
                                        else MaterialTheme.typography.titleMedium,
                                fontWeight = if (isKraftSub)
                                    androidx.compose.ui.text.font.FontWeight.Normal
                                else
                                    androidx.compose.ui.text.font.FontWeight.Bold,
                                color = if (isKraftSub)
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                else
                                    MaterialTheme.colorScheme.primary,
                                modifier = if (isKraftSub)
                                    Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
                                else
                                    Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(categoryExercises) { ex ->
                            val isReflection = ex.exerciseType in listOf("reflection_journal", "reflection_weekly")
                            val isRatings = ex.exerciseType == "ratings"
                            val isGame = ex.exerciseType != "standard" && !isReflection && !isRatings
                            ExerciseCard(
                                exercise = ex,
                                editable = true,
                                editLabel = if (selectedTab == 0) s.edit else s.editImages,
                                onEdit = { navController.navigate(Screen.ExerciseEditor.createRoute(ex.id)) },
                                onTest = when {
                                    isReflection -> { { reflectionTestExercise = ex } }
                                    isRatings -> { { ratingsTestExercise = ex } }
                                    isGame -> {
                                        {
                                            navController.navigate(
                                                Screen.ReactionGame.createRoute(
                                                    exerciseType = ex.exerciseType,
                                                    exerciseId = ex.id,
                                                    roundSeconds = 45,
                                                    rounds = 3,
                                                    param2 = if (ex.param2 != 0) ex.param2 else when (ex.exerciseType) {
                                                        "circle_overlap" -> 5
                                                        "color_reaction" -> 3
                                                        else -> 0
                                                    },
                                                    param3 = if (ex.param3 != 0) ex.param3 else when (ex.exerciseType) {
                                                        "circle_overlap" -> 2
                                                        "color_reaction" -> 3
                                                        else -> 0
                                                    }
                                                )
                                            )
                                        }
                                    }
                                    else -> null
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Reflection test dialog
    reflectionTestExercise?.let { ex ->
        val lang = LocalAppLanguage.current
        var text by remember { mutableStateOf("") }
        var saved by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { reflectionTestExercise = null; text = ""; saved = false },
            title = { Text(ex.localName(lang), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ex.localDescription(lang), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = text, onValueChange = { text = it; saved = false },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("Hier schreiben …") },
                        maxLines = 8, shape = RoundedCornerShape(10.dp)
                    )
                    if (saved) {
                        Text("In Trainingsnotizen gespeichert",
                            style = MaterialTheme.typography.bodySmall, color = TealPrimary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (text.isNotBlank()) { notesVm.addNote(text); saved = true }
                }, enabled = text.isNotBlank()) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { reflectionTestExercise = null; text = ""; saved = false }) {
                    Text("Schließen")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Ratings test dialog
    ratingsTestExercise?.let { ex ->
        val items = ex.ratingItems
        val scale = ex.ratingScale.coerceIn(2, 10)
        var ratings by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
        AlertDialog(
            onDismissRequest = { ratingsTestExercise = null },
            title = { Text(ex.nameDE, fontWeight = FontWeight.Bold) },
            text = {
                if (items.isEmpty()) {
                    Text("Keine Items konfiguriert. Items werden im Trainingsplan eingestellt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.forEach { item ->
                            val current = ratings[item] ?: 0
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    (1..scale).forEach { n ->
                                        val sel = current == n
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (sel) TealPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.weight(1f).clickable {
                                                ratings = ratings.toMutableMap().also { it[item] = n }
                                            }
                                        ) {
                                            Box(contentAlignment = Alignment.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)) {
                                                Text("$n", style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (sel) androidx.compose.ui.graphics.Color.White
                                                        else MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { ratingsTestExercise = null }) { Text("Schließen") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ExerciseCard(
    exercise: ExerciseTemplate,
    editable: Boolean,
    editLabel: String = "Bearbeiten",
    onEdit: () -> Unit = {},
    onTest: (() -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val lang = LocalAppLanguage.current
    val s = LocalAppStrings.current
    val name = exercise.localName(lang)
    val description = exercise.localDescription(lang)
    val isGame = exercise.exerciseType != "standard"

    val infoText: String? = when (exercise.exerciseType) {
        "ratings" -> if (lang == com.practicedyad.app.data.model.AppLanguage.ENGLISH)
            "Coaches can add items and set the scale (1–N) in the training plan. Athletes rate the items. Results are saved with the workout and can be viewed by the coach if sharing is enabled."
        else
            "Coaches können im Trainingsplan Items eintragen und die Skala festlegen (1–N). Athlet*innen bewerten die Items. Die Ergebnisse werden mit dem Workout gespeichert und können vom Coach eingesehen werden, wenn Teilen aktiviert ist."
        "color_reaction" -> if (lang == com.practicedyad.app.data.model.AppLanguage.ENGLISH)
            "At varying intervals, different colors are shown on the screen. The coach defines which task to perform for each color — e.g. run to a specific cone or move a specific body part. The coach sets round duration, average interval, and number of colors. Performance of this exercise is not automatically tracked."
        else
            "In variierenden Abständen werden verschiedene Farben auf dem Bildschirm angezeigt. Der Coach legt fest, welche Aufgabe bei welcher Farbe ausgeführt werden soll – z.B. zu einem bestimmten Hütchen laufen oder ein bestimmtes Körperteil bewegen. Der Coach stellt Rundenzeit, Durchschnittsintervall und Anzahl der Farben ein. Die Performance dieser Übung wird nicht automatisch getrackt."
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (exercise.photoUrls.isNotEmpty()) {
                    AsyncImage(
                        model = exercise.photoUrls.first(),
                        contentDescription = name,
                        modifier = Modifier.size(56.dp, 48.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(Modifier.width(12.dp))
                } else if (isGame) {
                    Box(
                        modifier = Modifier.size(56.dp, 48.dp)
                            .background(TealPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.SportsScore, null, tint = TealPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                        if (infoText != null) {
                            IconButton(onClick = { showInfo = true }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Info, "Info", modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    val displayCats = exercise.categories.ifEmpty { listOf(exercise.category) }.filter { it.isNotEmpty() }
                    if (displayCats.isNotEmpty()) {
                        Text(displayCats.joinToString(", ") { s.translateCategory(it) },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (isGame && onTest != null) {
                    IconButton(onClick = onTest, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.PlayArrow, "Testen", modifier = Modifier.size(20.dp), tint = TealPrimary)
                    }
                }
                if (editable) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                if (exercise.photoUrls.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        exercise.photoUrls.forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = name,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(4f / 3f)
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
                if (description.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(description, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showInfo && infoText != null) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text(name, fontWeight = FontWeight.Bold) },
            text = { Text(infoText, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("OK") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
