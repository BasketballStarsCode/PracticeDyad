package com.practicedyad.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.practicedyad.app.data.model.*
import com.practicedyad.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<ProgressEntry>>(emptyList())
    val entries: StateFlow<List<ProgressEntry>> = _entries.asStateFlow()

    private val _period = MutableStateFlow(ProgressPeriod.SIX_MONTHS)
    val period: StateFlow<ProgressPeriod> = _period.asStateFlow()

    private val _selectedExercises = MutableStateFlow<Set<String>>(emptySet())
    val selectedExercises: StateFlow<Set<String>> = _selectedExercises.asStateFlow()

    val filteredEntries: StateFlow<List<ProgressEntry>> = combine(_entries, _period, _selectedExercises) { entries, period, selected ->
        val cutoff = cutoffTimestamp(period)
        entries.filter { entry ->
            (selected.isEmpty() || entry.exerciseId in selected) &&
                    entry.date.seconds >= cutoff
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val exerciseNames: StateFlow<Map<String, String>> = _entries.map { entries ->
        entries.associate { it.exerciseId to it.exerciseName }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun load(athleteId: String) {
        val id = athleteId.ifEmpty { repo.currentUserId }
        viewModelScope.launch {
            repo.getAthleteProgress(id).collect { _entries.value = it }
        }
    }

    fun setPeriod(period: ProgressPeriod) { _period.value = period }

    fun toggleExercise(exerciseId: String) {
        _selectedExercises.update { current ->
            if (exerciseId in current) current - exerciseId else current + exerciseId
        }
    }

    fun selectAllExercises() {
        _selectedExercises.value = _entries.value.map { it.exerciseId }.toSet()
    }

    fun clearSelection() { _selectedExercises.value = emptySet() }

    private fun cutoffTimestamp(period: ProgressPeriod): Long {
        val cal = Calendar.getInstance()
        when (period) {
            ProgressPeriod.ONE_MONTH    -> cal.add(Calendar.MONTH, -1)
            ProgressPeriod.SIX_MONTHS  -> cal.add(Calendar.MONTH, -6)
            ProgressPeriod.ONE_YEAR    -> cal.add(Calendar.YEAR, -1)
            ProgressPeriod.ALL         -> return 0L
        }
        return cal.timeInMillis / 1000
    }
}
