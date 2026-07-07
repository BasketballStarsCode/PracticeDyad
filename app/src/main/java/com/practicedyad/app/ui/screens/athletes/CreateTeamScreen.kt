package com.practicedyad.app.ui.screens.athletes

import androidx.compose.foundation.clickable
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
import com.practicedyad.app.data.model.Team
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.viewmodel.AthleteViewModel
import java.util.UUID

@Composable
fun CreateTeamScreen(
    navController: NavController,
    editTeamId: String,
    vm: AthleteViewModel = hiltViewModel()
) {
    val athletes by vm.athletes.collectAsStateWithLifecycle()
    val teams by vm.teams.collectAsStateWithLifecycle()
    val existingTeam = teams.find { it.id == editTeamId }

    var teamName by remember { mutableStateOf(existingTeam?.name ?: "") }
    var selectedAthleteIds by remember { mutableStateOf(existingTeam?.athleteIds?.toSet() ?: emptySet()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadAthletes()
        vm.loadTeams()
    }

    Scaffold(
        topBar = {
            PDTopBar(
                title = if (editTeamId.isEmpty()) "Team erstellen" else "Team bearbeiten",
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editTeamId.isNotEmpty()) {
                    PDButton(
                        "Team löschen",
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.DANGER
                    )
                }
                PDButton(
                    "Fertig",
                    onClick = {
                        val team = Team(
                            id = editTeamId.ifEmpty { UUID.randomUUID().toString() },
                            coachId = vm.currentUserId,
                            name = teamName,
                            athleteIds = selectedAthleteIds.toList()
                        )
                        vm.saveTeam(team) { navController.popBackStack() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = teamName.isNotBlank()
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PDTextField(value = teamName, onValueChange = { teamName = it }, label = "Teamname")
            }

            item { PDSectionHeader("Athlet*innen zuordnen") }

            items(athletes) { athlete ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedAthleteIds = if (athlete.id in selectedAthleteIds)
                            selectedAthleteIds - athlete.id
                        else selectedAthleteIds + athlete.id
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = athlete.id in selectedAthleteIds,
                        onCheckedChange = null
                    )
                    Spacer(Modifier.width(8.dp))
                    AvatarCircle(athlete.name, size = 36)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(athlete.name, fontWeight = FontWeight.Medium)
                        Text(athlete.email, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        PDConfirmDialog(
            title = "Team löschen",
            message = "Das Team wird dauerhaft gelöscht.",
            confirmText = "Löschen",
            onConfirm = { vm.deleteTeam(editTeamId); navController.popBackStack() },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
