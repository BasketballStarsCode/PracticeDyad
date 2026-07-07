package com.practicedyad.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.practicedyad.app.data.model.TrainingNote
import com.practicedyad.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class TrainingNotesViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<TrainingNote>>(emptyList())
    val notes: StateFlow<List<TrainingNote>> = _notes.asStateFlow()

    init {
        val uid = repo.currentUserId
        if (uid.isNotEmpty()) {
            viewModelScope.launch {
                repo.getTrainingNotes(uid).collect { _notes.value = it }
            }
        }
    }

    fun addNote(text: String) {
        val uid = repo.currentUserId
        if (uid.isEmpty()) return
        val note = TrainingNote(
            id = UUID.randomUUID().toString(),
            athleteId = uid,
            text = text,
            createdAt = Timestamp.now()
        )
        viewModelScope.launch { repo.addTrainingNote(note) }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch { repo.deleteTrainingNote(noteId) }
    }
}
