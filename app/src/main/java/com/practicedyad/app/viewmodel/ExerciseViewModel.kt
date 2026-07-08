package com.practicedyad.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicedyad.app.data.model.*
import com.practicedyad.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    val currentUserId: String get() = repo.currentUserId

    private val _customExercises = MutableStateFlow<List<ExerciseTemplate>>(emptyList())
    val customExercises: StateFlow<List<ExerciseTemplate>> = _customExercises.asStateFlow()

    private val _standardExercises = MutableStateFlow<List<ExerciseTemplate>>(emptyList())
    val standardExercises: StateFlow<List<ExerciseTemplate>> = _standardExercises.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    val filteredCustom: StateFlow<List<ExerciseTemplate>> = combine(_customExercises, _searchQuery) { list, q ->
        if (q.isBlank()) list else list.filter {
            it.nameDE.contains(q, true) || it.nameEN.contains(q, true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val filteredStandard: StateFlow<List<ExerciseTemplate>> = combine(_standardExercises, _searchQuery) { list, q ->
        if (q.isBlank()) list else list.filter {
            it.nameDE.contains(q, true) || it.nameEN.contains(q, true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadExercises() {
        viewModelScope.launch {
            repo.getExerciseTemplates(repo.currentUserId).collect { _customExercises.value = it }
        }
        viewModelScope.launch {
            repo.getStandardExercises().collect { _standardExercises.value = it }
        }
    }

    private val _targetTemplateId = MutableStateFlow("")

    // Reaktiv: sobald Übungen geladen sind und eine ID gesetzt ist, wird das Template gefunden
    val selectedTemplate: StateFlow<ExerciseTemplate?> = combine(
        _customExercises, _standardExercises, _targetTemplateId
    ) { custom, standard, id ->
        if (id.isEmpty()) null else (custom + standard).find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun loadTemplate(templateId: String) {
        if (templateId.isEmpty()) return
        _targetTemplateId.value = templateId
        loadExercises()
    }

    fun setSearch(query: String) { _searchQuery.value = query }

    fun saveExercise(
        template: ExerciseTemplate,
        imageUris: List<Uri>,
        keepUrls: List<String> = emptyList(),
        context: Context,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val uploadedUrls = imageUris.mapNotNull { uri ->
                    val bytes = context.contentResolver.openInputStream(uri)?.readBytes() ?: return@mapNotNull null
                    val path = "exercise_images/${repo.currentUserId}/${UUID.randomUUID()}.jpg"
                    repo.uploadImage(bytes, path)
                }
                repo.saveExerciseTemplate(template.copy(
                    id = template.id.ifEmpty { UUID.randomUUID().toString() },
                    coachId = repo.currentUserId,
                    isCustom = true,
                    photoUrls = keepUrls + uploadedUrls
                ))
                onDone()
            } catch (e: Exception) {
                // handle error
            } finally {
                _loading.value = false
            }
        }
    }
}

