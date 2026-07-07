package com.practicedyad.app.ui.screens.organization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.ConceptSection
import com.practicedyad.app.data.model.TrainingConcept
import com.practicedyad.app.ui.components.PDCard
import com.practicedyad.app.ui.components.PDSectionHeader
import com.practicedyad.app.ui.components.PDTopBar
import com.practicedyad.app.viewmodel.SettingsViewModel

@Composable
fun TrainingConceptScreen(
    navController: NavController,
    vm: SettingsViewModel = hiltViewModel()
) {
    val org by vm.organization.collectAsStateWithLifecycle()
    val concept = org?.trainingConcept

    var title by remember(concept) { mutableStateOf(concept?.title ?: "") }
    var sections by remember(concept) {
        mutableStateOf(concept?.sections ?: listOf(ConceptSection()))
    }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { PDTopBar("Trainingskonzept", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                val updated = org?.copy(
                    trainingConcept = TrainingConcept(title = title, sections = sections)
                )
                if (updated != null) {
                    vm.saveOrganization(updated)
                    saved = true
                }
            }) {
                Text("Speichern", modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (saved) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "Gespeichert",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            PDCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Titel", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; saved = false },
                        placeholder = { Text("z. B. Unser Trainingsansatz") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            PDSectionHeader("Abschnitte")

            sections.forEachIndexed { idx, section ->
                PDCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Abschnitt ${idx + 1}", fontWeight = FontWeight.SemiBold)
                            if (sections.size > 1) {
                                IconButton(onClick = {
                                    sections = sections.toMutableList().also { it.removeAt(idx) }
                                    saved = false
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        OutlinedTextField(
                            value = section.heading,
                            onValueChange = { new ->
                                sections = sections.toMutableList().also {
                                    it[idx] = it[idx].copy(heading = new)
                                }
                                saved = false
                            },
                            label = { Text("Überschrift") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = section.body,
                            onValueChange = { new ->
                                sections = sections.toMutableList().also {
                                    it[idx] = it[idx].copy(body = new)
                                }
                                saved = false
                            },
                            label = { Text("Inhalt") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    sections = sections + ConceptSection()
                    saved = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Abschnitt hinzufügen")
            }

            Spacer(Modifier.height(72.dp))
        }
    }
}
