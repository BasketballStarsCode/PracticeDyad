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
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.LocalAppLanguage
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.ui.theme.localDescription
import com.practicedyad.app.ui.theme.localName
import com.practicedyad.app.ui.theme.translateCategory
import com.practicedyad.app.viewmodel.ExerciseViewModel

@Composable
fun MyExercisesScreen(
    navController: NavController,
    vm: ExerciseViewModel = hiltViewModel()
) {
    val customExercises by vm.filteredCustom.collectAsStateWithLifecycle()
    val standardExercises by vm.filteredStandard.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { vm.loadExercises() }

    val s = LocalAppStrings.current

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
                val grouped = exercises.sortedBy { it.category }.groupBy { it.category }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    grouped.forEach { (category, categoryExercises) ->
                        item {
                            Text(
                                s.translateCategory(category),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(categoryExercises) { ex ->
                            ExerciseCard(
                                exercise = ex,
                                editable = true,
                                editLabel = if (selectedTab == 0) s.edit else s.editImages,
                                onEdit = { navController.navigate(Screen.ExerciseEditor.createRoute(ex.id)) },
                                onTest = if (ex.exerciseType != "standard") {
                                    {
                                        navController.navigate(
                                            Screen.ReactionGame.createRoute(
                                                exerciseType = ex.exerciseType,
                                                exerciseId = ex.id,
                                                roundSeconds = 60,
                                                rounds = 3,
                                                param2 = when (ex.exerciseType) {
                                                    "circle_overlap" -> 5
                                                    "color_reaction" -> 3
                                                    else -> 0
                                                },
                                                param3 = when (ex.exerciseType) {
                                                    "circle_overlap" -> 2
                                                    "color_reaction" -> 3
                                                    else -> 0
                                                }
                                            )
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
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
    val lang = LocalAppLanguage.current
    val s = LocalAppStrings.current
    val name = exercise.localName(lang)
    val description = exercise.localDescription(lang)
    val isGame = exercise.exerciseType != "standard"

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
                    Text(name, fontWeight = FontWeight.SemiBold)
                    if (exercise.category.isNotEmpty()) {
                        Text(s.translateCategory(exercise.category), style = MaterialTheme.typography.bodySmall,
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

            if (expanded && description.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
