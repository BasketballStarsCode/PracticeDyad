package com.practicedyad.app.ui.screens.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.AppUser
import com.practicedyad.app.data.model.ChatConversation
import com.practicedyad.app.data.model.UserRole
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.viewmodel.AthleteViewModel
import com.practicedyad.app.viewmodel.AuthViewModel
import com.practicedyad.app.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    navController: NavController,
    vm: ChatViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel(),
    athleteVm: AthleteViewModel = hiltViewModel()
) {
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val athletes by athleteVm.athletes.collectAsStateWithLifecycle()
    val user by authVm.currentUser.collectAsStateWithLifecycle()
    val isCoach = user?.role == UserRole.COACH || user?.role == UserRole.BOTH

    var showGroupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.loadConversations()
        athleteVm.loadAthletes()
    }

    Scaffold(
        topBar = { PDTopBar("Chat", onBack = { navController.popBackStack() }) },
        floatingActionButton = {
            if (isCoach) {
                FloatingActionButton(
                    onClick = { showGroupDialog = true },
                    containerColor = TealPrimary
                ) { Icon(Icons.Default.Group, "Gruppenchat erstellen", tint = Color.White) }
            }
        }
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState("Noch keine Gespräche.\nSchreibe deinen Coach oder Athlet*innen.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations) { conv ->
                    if (conv.isGroup) {
                        ConversationCard(
                            name = conv.name.ifEmpty { "Gruppe" },
                            lastMessage = conv.lastMessage,
                            timestamp = conv.lastMessageAt.toDate(),
                            isGroup = true,
                            onClick = { navController.navigate(Screen.Chat.createRoute(conv.id, "")) }
                        )
                    } else {
                        val otherId = conv.participantIds.firstOrNull { it != user?.id } ?: ""
                        val otherUser = athletes.find { it.id == otherId }
                        val otherName = otherUser?.name ?: "Unbekannt"
                        ConversationCard(
                            name = otherName,
                            lastMessage = conv.lastMessage,
                            timestamp = conv.lastMessageAt.toDate(),
                            onClick = { navController.navigate(Screen.Chat.createRoute(conv.id, otherId)) }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showGroupDialog) {
        GroupChatDialog(
            athletes = athletes,
            onCreate = { name, memberIds ->
                vm.createGroupChat(name, memberIds) { convId ->
                    showGroupDialog = false
                    navController.navigate(Screen.Chat.createRoute(convId, ""))
                }
            },
            onDismiss = { showGroupDialog = false }
        )
    }
}

@Composable
fun GroupChatDialog(
    athletes: List<AppUser>,
    onCreate: (String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gruppenchat erstellen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Gruppenname") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Text("Teilnehmer*innen wählen:", style = MaterialTheme.typography.labelLarge)
                athletes.forEach { athlete ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selected = if (athlete.id in selected) selected - athlete.id else selected + athlete.id
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = athlete.id in selected, onCheckedChange = null)
                        Text(athlete.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (groupName.isNotBlank()) onCreate(groupName, selected.toList()) },
                enabled = groupName.isNotBlank() && selected.isNotEmpty()
            ) { Text("Erstellen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ConversationCard(name: String, lastMessage: String, timestamp: Date, isGroup: Boolean = false, onClick: () -> Unit) {
    val fmt = SimpleDateFormat("HH:mm", Locale.GERMAN)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isGroup) {
                Icon(Icons.Default.Group, null,
                    modifier = Modifier.size(40.dp),
                    tint = TealPrimary)
            } else {
                AvatarCircle(name)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(lastMessage, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(fmt.format(timestamp), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
