package com.practicedyad.app.ui.screens.athletes

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.*
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.viewmodel.AthleteViewModel

@Composable
fun AthletesScreen(
    navController: NavController,
    vm: AthleteViewModel = hiltViewModel()
) {
    val athletes by vm.athletes.collectAsStateWithLifecycle()
    val teams by vm.teams.collectAsStateWithLifecycle()
    val connectCode by vm.connectCode.collectAsStateWithLifecycle()
    val clipboard: ClipboardManager = LocalClipboardManager.current

    var showAddMenu by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    val expandedTeams = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        vm.loadAthletes()
        vm.loadTeams()
        vm.loadCurrentUserCode()
    }

    Scaffold(
        topBar = { PDTopBar("Athlet*innen", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showAddMenu = true }, containerColor = TealPrimary) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
                DropdownMenu(expanded = showAddMenu, onDismissRequest = { showAddMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Athlet*in hinzufügen") },
                        leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                        onClick = { showAddMenu = false; showCodeDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Team hinzufügen") },
                        leadingIcon = { Icon(Icons.Default.Group, null) },
                        onClick = {
                            showAddMenu = false
                            navController.navigate(Screen.CreateTeam.createRoute())
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Teams
            items(teams) { team ->
                TeamCard(
                    team = team,
                    athletes = athletes,
                    expanded = expandedTeams[team.id] == true,
                    onToggle = { expandedTeams[team.id] = !(expandedTeams[team.id] ?: false) },
                    onEdit = { navController.navigate(Screen.CreateTeam.createRoute(team.id)) },
                    onAthleteClick = { navController.navigate(Screen.AthleteDetail.createRoute(it)) }
                )
            }

            // Individual athletes (not in any team)
            val teamAthleteIds = teams.flatMap { it.athleteIds }.toSet()
            val soloAthletes = athletes.filter { it.id !in teamAthleteIds }
            items(soloAthletes) { athlete ->
                AthleteCard(
                    athlete = athlete,
                    onClick = { navController.navigate(Screen.AthleteDetail.createRoute(athlete.id)) }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showCodeDialog) {
        AlertDialog(
            onDismissRequest = { showCodeDialog = false },
            title = { Text("Verbindungscode") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Teile diesen Code mit deinen Athlet*innen, damit sie sich mit dir verbinden können:")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(connectCode, style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = TealPrimary)
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(connectCode))
                            }) { Icon(Icons.Default.ContentCopy, "Kopieren") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showCodeDialog = false }) { Text("Fertig") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun TeamCard(
    team: Team,
    athletes: List<AppUser>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onAthleteClick: (String) -> Unit
) {
    val teamAthletes = athletes.filter { it.id in team.athleteIds }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Group, null, tint = TealPrimary)
                Spacer(Modifier.width(8.dp))
                Text(team.name, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${teamAthletes.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Bearbeiten", modifier = Modifier.size(18.dp), tint = TealPrimary)
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    teamAthletes.forEach { athlete ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onAthleteClick(athlete.id) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarCircle(athlete.name, size = 36)
                            Spacer(Modifier.width(12.dp))
                            Text(athlete.name, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AthleteCard(athlete: AppUser, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(athlete.name)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(athlete.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(athlete.email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
