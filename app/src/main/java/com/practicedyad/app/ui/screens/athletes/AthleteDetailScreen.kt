package com.practicedyad.app.ui.screens.athletes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.viewmodel.AthleteViewModel
import com.practicedyad.app.viewmodel.ChatViewModel

@Composable
fun AthleteDetailScreen(
    navController: NavController,
    athleteId: String,
    vm: AthleteViewModel = hiltViewModel(),
    chatVm: ChatViewModel = hiltViewModel()
) {
    val athlete by vm.selectedAthlete.collectAsStateWithLifecycle()
    val conversationId by chatVm.conversationId.collectAsStateWithLifecycle()

    LaunchedEffect(athleteId) { vm.loadAthlete(athleteId) }

    LaunchedEffect(conversationId) {
        if (conversationId.isNotEmpty()) {
            navController.navigate(Screen.Chat.createRoute(conversationId, athleteId))
        }
    }

    Scaffold(
        topBar = {
            PDTopBar(
                title = athlete?.name ?: "Athlet*in",
                onBack = { navController.popBackStack() }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                // Profile header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarCircle(athlete?.name ?: "?", size = 64)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(athlete?.name ?: "", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold)
                        Text(athlete?.email ?: "", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PDButton(
                        text = "Nachrichten",
                        onClick = { chatVm.openConversation(athleteId) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.SECONDARY
                    )
                    PDButton(
                        text = "Durchgeführte Workouts",
                        onClick = { navController.navigate(Screen.SharedWorkouts.createRoute(athleteId)) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.SECONDARY
                    )
                    PDButton(
                        text = "Trainingsfortschritt",
                        onClick = { navController.navigate(Screen.Progress.createRoute(athleteId)) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.SECONDARY
                    )
                }
            }
        }
    }
}
