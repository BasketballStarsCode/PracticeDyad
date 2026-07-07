package com.practicedyad.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.practicedyad.app.data.model.*
import com.practicedyad.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    val currentUserId: String get() = repo.currentUserId

    private val _conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _conversationId = MutableStateFlow("")
    val conversationId: StateFlow<String> = _conversationId.asStateFlow()

    fun loadConversations() {
        viewModelScope.launch {
            repo.getConversations(repo.currentUserId).collect { _conversations.value = it }
        }
    }

    fun openConversation(otherUserId: String) {
        viewModelScope.launch {
            val convId = repo.getOrCreateConversation(repo.currentUserId, otherUserId)
            _conversationId.value = convId
            repo.getMessages(convId).collect { _messages.value = it }
        }
    }

    fun openConversationById(conversationId: String) {
        _conversationId.value = conversationId
        viewModelScope.launch {
            repo.getMessages(conversationId).collect { _messages.value = it }
        }
    }

    private val _pinnedMessages = MutableStateFlow<List<com.practicedyad.app.data.model.ChatMessage>>(emptyList())
    val pinnedMessages: StateFlow<List<com.practicedyad.app.data.model.ChatMessage>> = _pinnedMessages.asStateFlow()

    fun editMessage(messageId: String, newContent: String) {
        val convId = _conversationId.value
        if (convId.isEmpty()) return
        viewModelScope.launch { repo.editMessage(convId, messageId, newContent) }
    }

    fun pinMessage(messageId: String, pinned: Boolean) {
        val convId = _conversationId.value
        if (convId.isEmpty()) return
        viewModelScope.launch { repo.pinMessage(convId, messageId, pinned) }
    }

    fun createGroupChat(name: String, memberIds: List<String>, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val convId = repo.createGroupConversation(name, memberIds + repo.currentUserId)
            onDone(convId)
        }
    }

    fun sendMessage(text: String) {
        val convId = _conversationId.value
        if (convId.isEmpty() || text.isBlank()) return
        viewModelScope.launch {
            repo.sendMessage(
                convId,
                ChatMessage(
                    conversationId = convId,
                    senderId = repo.currentUserId,
                    content = text.trim(),
                    timestamp = Timestamp.now()
                )
            )
        }
    }
}
