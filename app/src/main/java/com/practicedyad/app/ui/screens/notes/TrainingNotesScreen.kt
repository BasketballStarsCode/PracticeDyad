package com.practicedyad.app.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.practicedyad.app.ui.components.PDTopBar
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.viewmodel.TrainingNotesViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrainingNotesScreen(
    navController: NavController,
    vm: TrainingNotesViewModel = hiltViewModel()
) {
    val s = LocalAppStrings.current
    val notes by vm.notes.collectAsState()
    var showNewNote by remember { mutableStateOf(false) }
    var newText by remember { mutableStateOf("") }

    Scaffold(
        topBar = { PDTopBar(s.navTrainingNotes, onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewNote = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s.noNotesYet,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    SimpleDateFormat("d. MMM yyyy", Locale.getDefault()).format(note.createdAt.toDate()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                IconButton(onClick = { vm.deleteNote(note.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(note.text, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (showNewNote) {
        AlertDialog(
            onDismissRequest = { showNewNote = false; newText = "" },
            title = { Text(s.newNote, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    placeholder = { Text(s.writeNote) },
                    maxLines = 8
                )
            },
            confirmButton = {
                Button(
                    onClick = { if (newText.isNotBlank()) { vm.addNote(newText); newText = ""; showNewNote = false } },
                    enabled = newText.isNotBlank()
                ) { Text(s.save) }
            },
            dismissButton = {
                TextButton(onClick = { showNewNote = false; newText = "" }) { Text(s.cancel) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
