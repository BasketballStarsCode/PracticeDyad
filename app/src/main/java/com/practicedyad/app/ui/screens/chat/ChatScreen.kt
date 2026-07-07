package com.practicedyad.app.ui.screens.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.ChatMessage
import com.practicedyad.app.ui.components.PDTopBar
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.viewmodel.AthleteViewModel
import com.practicedyad.app.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    navController: NavController,
    conversationId: String,
    otherUserId: String,
    vm: ChatViewModel = hiltViewModel(),
    athleteVm: AthleteViewModel = hiltViewModel()
) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val athletes by athleteVm.athletes.collectAsStateWithLifecycle()
    val otherUser = athletes.find { it.id == otherUserId }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Pinned messages
    val pinnedMessages = messages.filter { it.pinned }

    // Edit state
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var contextMessage by remember { mutableStateOf<ChatMessage?>(null) }

    LaunchedEffect(conversationId) {
        vm.openConversationById(conversationId)
        athleteVm.loadAthletes()
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            Column {
                PDTopBar(
                    title = otherUser?.name ?: "Chat",
                    onBack = { navController.popBackStack() }
                )
                // Pinned messages banner
                if (pinnedMessages.isNotEmpty()) {
                    Surface(
                        color = TealPrimary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PushPin, null, tint = TealPrimary,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                pinnedMessages.last().content,
                                style = MaterialTheme.typography.bodySmall,
                                color = TealPrimary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column {
                // Edit indicator
                if (editingMessage != null) {
                    Surface(color = TealPrimary.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, null, tint = TealPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Nachricht bearbeiten", style = MaterialTheme.typography.bodySmall,
                                color = TealPrimary, modifier = Modifier.weight(1f))
                            IconButton(onClick = { editingMessage = null; inputText = "" },
                                modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Nachricht …") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val editing = editingMessage
                                if (editing != null) {
                                    vm.editMessage(editing.id, inputText)
                                    editingMessage = null
                                } else {
                                    vm.sendMessage(inputText)
                                }
                                inputText = ""
                            }
                        },
                        containerColor = TealPrimary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(if (editingMessage != null) Icons.Default.Check else Icons.Default.Send, null)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                MessageBubble(
                    message = msg,
                    isOwnMessage = msg.senderId == vm.currentUserId,
                    onLongPress = { contextMessage = msg }
                )
            }
        }
    }

    // Context menu dialog
    contextMessage?.let { msg ->
        val isOwn = msg.senderId == vm.currentUserId
        AlertDialog(
            onDismissRequest = { contextMessage = null },
            title = null,
            text = {
                Column {
                    if (isOwn) {
                        TextButton(onClick = {
                            editingMessage = msg
                            inputText = msg.content
                            contextMessage = null
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Bearbeiten")
                        }
                    }
                    TextButton(onClick = {
                        vm.pinMessage(msg.id, !msg.pinned)
                        contextMessage = null
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PushPin, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (msg.pinned) "Anpinnen aufheben" else "Anpinnen")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMessage = null }) { Text("Abbrechen") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean,
    onLongPress: () -> Unit
) {
    val fmt = SimpleDateFormat("HH:mm", Locale.GERMAN)
    val align = if (isOwnMessage) Alignment.End else Alignment.Start
    val bgColor = if (isOwnMessage) TealPrimary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isOwnMessage) Color.White else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(
        topStart = 16.dp, topEnd = 16.dp,
        bottomStart = if (isOwnMessage) 16.dp else 4.dp,
        bottomEnd = if (isOwnMessage) 4.dp else 16.dp
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        if (message.pinned) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)) {
                Icon(Icons.Default.PushPin, null, tint = TealPrimary, modifier = Modifier.size(12.dp))
                Text(" Angepinnt", style = MaterialTheme.typography.labelSmall, color = TealPrimary)
            }
        }
        Surface(
            shape = shape, color = bgColor,
            modifier = Modifier.widthIn(max = 280.dp).combinedClickable(
                onClick = {},
                onLongClick = onLongPress
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(message.content, color = textColor)
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    if (message.edited) {
                        Text("bearbeitet  ", style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                    Text(
                        fmt.format(message.timestamp.toDate()),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
