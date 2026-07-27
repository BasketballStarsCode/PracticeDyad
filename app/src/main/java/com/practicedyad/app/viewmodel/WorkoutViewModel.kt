package com.practicedyad.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.practicedyad.app.data.model.*
import com.practicedyad.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repo: AppRepository
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<WorkoutSession>>(emptyList())
    val sessions: StateFlow<List<WorkoutSession>> = _sessions.asStateFlow()

    private val _activeSession = MutableStateFlow<WorkoutSession?>(null)
    val activeSession: StateFlow<WorkoutSession?> = _activeSession.asStateFlow()

    private val _currentExerciseIndex = MutableStateFlow(0)
    val currentExerciseIndex: StateFlow<Int> = _currentExerciseIndex.asStateFlow()

    private val _lastWeights = MutableStateFlow<Map<String, Float>>(emptyMap())
    val lastWeights: StateFlow<Map<String, Float>> = _lastWeights.asStateFlow()

    private val _exercisePreferences = MutableStateFlow<Map<String, Int>>(emptyMap())
    val exercisePreferences: StateFlow<Map<String, Int>> = _exercisePreferences.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(0L)
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun loadSessions(athleteId: String) {
        viewModelScope.launch {
            repo.getAthleteSessions(athleteId).collect { _sessions.value = it }
        }
    }

    fun loadExercisePreferences() {
        viewModelScope.launch {
            repo.getExercisePreferences(repo.currentUserId).collect { prefs ->
                _exercisePreferences.value = prefs.associate { it.exerciseId to it.activeIndex }
            }
        }
    }

    fun setExercisePreference(exerciseId: String, activeIndex: Int) {
        viewModelScope.launch {
            val prefId = "${repo.currentUserId}_$exerciseId"
            repo.saveExercisePreference(
                ExercisePreference(id = prefId, athleteId = repo.currentUserId,
                    exerciseId = exerciseId, activeIndex = activeIndex)
            )
            _exercisePreferences.value = _exercisePreferences.value + (exerciseId to activeIndex)
        }
    }

    fun startSession(unit: WorkoutUnit, plan: TrainingPlan) {
        val entries = unit.exercises.map { ex ->
            ExerciseEntry(
                exerciseId = ex.id,
                exerciseName = ex.customName,
                sets = (1..ex.sets).map { i ->
                    SetEntry(setNumber = i, reps = ex.reps, durationSeconds = ex.durationSeconds)
                }
            )
        }
        _activeSession.value = WorkoutSession(
            athleteId = repo.currentUserId,
            coachId = plan.coachId,
            planId = plan.id,
            workoutUnitId = unit.id,
            workoutUnitName = unit.name,
            date = Timestamp.now(),
            exerciseEntries = entries
        )
        _currentExerciseIndex.value = 0
        _sessionStartTime.value = System.currentTimeMillis()
        viewModelScope.launch { loadLastWeights(unit.exercises.map { it.id }) }
    }

    private suspend fun loadLastWeights(exerciseIds: List<String>) {
        val map = mutableMapOf<String, Float>()
        exerciseIds.forEach { id ->
            repo.getLastWeight(repo.currentUserId, id)?.let { map[id] = it.weight }
        }
        _lastWeights.value = map
    }

    fun updateSetEntry(exerciseIndex: Int, setIndex: Int, weight: Float?, reps: Int?, duration: Int?) {
        val session = _activeSession.value ?: return
        val entries = session.exerciseEntries.toMutableList()
        val entry = entries[exerciseIndex]
        val sets = entry.sets.toMutableList()
        val set = sets[setIndex]
        sets[setIndex] = set.copy(
            weight = weight ?: set.weight,
            reps = reps ?: set.reps,
            durationSeconds = duration ?: set.durationSeconds,
            completed = true
        )
        entries[exerciseIndex] = entry.copy(sets = sets)
        _activeSession.value = session.copy(exerciseEntries = entries)
    }

    fun nextExercise() {
        val session = _activeSession.value ?: return
        if (_currentExerciseIndex.value < session.exerciseEntries.size - 1) {
            _currentExerciseIndex.value++
        }
    }

    fun previousExercise() {
        if (_currentExerciseIndex.value > 0) _currentExerciseIndex.value--
    }

    fun finishSession(shareWithCoach: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            val session = _activeSession.value ?: return@launch
            val elapsed = ((System.currentTimeMillis() - _sessionStartTime.value) / 60000).toInt()
            val finalSession = session.copy(
                completedAt = Timestamp.now(),
                sharedWithCoach = shareWithCoach,
                durationMinutes = elapsed
            )
            try {
                val id = repo.saveWorkoutSession(finalSession)
                // Save progress entries
                finalSession.exerciseEntries.forEach { entry ->
                    val maxWeight = entry.sets.maxOfOrNull { it.weight } ?: 0f
                    val totalReps = entry.sets.sumOf { it.reps }
                    val totalDuration = entry.sets.sumOf { it.durationSeconds }
                    if (maxWeight > 0 || totalReps > 0 || totalDuration > 0) {
                        repo.saveProgress(
                            ProgressEntry(
                                athleteId = repo.currentUserId,
                                exerciseId = entry.exerciseId,
                                exerciseName = entry.exerciseName,
                                date = Timestamp.now(),
                                maxWeight = maxWeight,
                                totalReps = totalReps,
                                totalDurationSeconds = totalDuration
                            )
                        )
                    }
                    // Save last used weight
                    val lastWeight = entry.sets.lastOrNull { it.weight > 0 }?.weight
                    if (lastWeight != null) {
                        repo.saveLastWeight(
                            LastUsedWeight(
                                athleteId = repo.currentUserId,
                                exerciseId = entry.exerciseId,
                                weight = lastWeight
                            )
                        )
                    }
                }
                _activeSession.value = null
                onDone()
            } catch (e: Exception) {
                // handle error
            } finally {
                _loading.value = false
            }
        }
    }

    fun saveGameResult(
        exerciseId: String,
        exerciseName: String,
        avgReactionMs: Long = 0,
        correctAttempts: Int = 0,
        wrongAttempts: Int = 0
    ) {
        viewModelScope.launch {
            repo.saveProgress(
                ProgressEntry(
                    athleteId = repo.currentUserId,
                    exerciseId = exerciseId,
                    exerciseName = exerciseName,
                    date = Timestamp.now(),
                    avgReactionMs = avgReactionMs,
                    correctAttempts = correctAttempts,
                    wrongAttempts = wrongAttempts
                )
            )
        }
    }

    fun updateRatings(exerciseIndex: Int, ratings: Map<String, Int>) {
        val session = _activeSession.value ?: return
        val entries = session.exerciseEntries.toMutableList()
        entries[exerciseIndex] = entries[exerciseIndex].copy(ratings = ratings)
        _activeSession.value = session.copy(exerciseEntries = entries)
    }

    fun getSharedSessions(coachId: String, athleteId: String): Flow<List<WorkoutSession>> =
        repo.getSharedSessions(coachId, athleteId)

    fun canStartWorkout(unit: WorkoutUnit, sessions: List<WorkoutSession>): Boolean {
        if (unit.minRestDays == 0) return true
        val last = sessions
            .filter { it.workoutUnitId == unit.id && it.completedAt != null }
            .maxByOrNull { it.completedAt!!.seconds }
        last ?: return true
        val daysSince = (Timestamp.now().seconds - last.completedAt!!.seconds) / 86400
        return daysSince >= unit.minRestDays
    }
}
