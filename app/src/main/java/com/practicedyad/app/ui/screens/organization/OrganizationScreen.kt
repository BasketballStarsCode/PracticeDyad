package com.practicedyad.app.ui.screens.organization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen

@Composable
fun OrganizationScreen(navController: NavController) {
    Scaffold(
        topBar = { PDTopBar("Organisation", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PDCard {
                Text("Organisationsverwaltung", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Hier kannst du Rechte vergeben und die Einstellungen für deine Organisation verwalten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            PDSectionHeader("Berechtigungen")
            PDCard {
                PDToggle(checked = false, onCheckedChange = {}, label = "Alle Coaches: Trainingskonzept bearbeiten")
                PDDivider()
                PDToggle(checked = false, onCheckedChange = {}, label = "Alle Coaches: Übungsdatenbank bearbeiten")
                PDDivider()
                PDToggle(checked = true, onCheckedChange = {}, label = "Athlet*innen können Fortschritt teilen")
                PDDivider()
                PDToggle(checked = true, onCheckedChange = {}, label = "Athlet*innen können Workouts teilen")
            }

            PDSectionHeader("Mitglieder")
            PDCard {
                Text("Coach-Zugangscode:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text("ORG-XXXXX", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            PDButton(
                "Trainingskonzept verwalten",
                onClick = { navController.navigate(Screen.TrainingConcept.route) },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.SECONDARY
            )
        }
    }
}

@Composable
fun TrainingConceptScreen(navController: NavController) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Scaffold(
        topBar = { PDTopBar("Trainingskonzept", onBack = { navController.popBackStack() }) },
        bottomBar = {
            PDButton("Speichern", onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PDTextField(title, { title = it }, "Überschrift")
            PDTextField(body, { body = it }, "Inhalt", singleLine = false, maxLines = 20)
            PDButton(
                "PDF hochladen", onClick = { },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.SECONDARY
            )
            Spacer(Modifier.height(80.dp))
        }
    }
}
