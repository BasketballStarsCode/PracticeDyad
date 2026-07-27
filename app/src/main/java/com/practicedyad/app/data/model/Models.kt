package com.practicedyad.app.data.model

import com.google.firebase.Timestamp

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class UserRole { COACH, ATHLETE, BOTH, ORGANIZATION }
enum class WeightUnit { KG, LBS }
enum class AppLanguage { GERMAN, ENGLISH }
enum class ProgressPeriod { ONE_MONTH, SIX_MONTHS, ONE_YEAR, ALL }
enum class SubscriptionTier(val maxAthletes: Int) {
    FREE(3), STANDARD(25), PROFESSIONAL(60), UNLIMITED(Int.MAX_VALUE)
}

// ─── User ────────────────────────────────────────────────────────────────────

data class AppUser(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.ATHLETE,
    val photoUrl: String = "",
    val coachIds: List<String> = emptyList(),
    val athleteIds: List<String> = emptyList(),
    val organizationId: String = "",
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val connectCode: String = "",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val language: AppLanguage = AppLanguage.GERMAN,
    val darkMode: Boolean = false,
    val notifyNewPlan: Boolean = true,
    val notifyWorkout: Boolean = true,
    val notifyWorkoutTime: String = "08:00",
    val notifyMessages: Boolean = true,
    val notifyCompletedWorkouts: Boolean = true,
    val shareProgressWithCoach: Boolean = false,
    val fcmToken: String = ""
)

// ─── Organization ─────────────────────────────────────────────────────────────

data class Organization(
    val id: String = "",
    val name: String = "",
    val coachLimit: Int = 10,
    val athleteLimit: Int = 25,
    val accessCode: String = "",
    val adminId: String = "",
    val coachIds: List<String> = emptyList(),
    val canEditConcept: List<String> = emptyList(),   // coach IDs with edit rights
    val canEditDatabase: List<String> = emptyList(),
    val allCoachesCanEditConcept: Boolean = false,
    val allCoachesCanEditDatabase: Boolean = false,
    val athletesCanShareProgress: Boolean = true,
    val athletesCanShareWorkouts: Boolean = true,
    val trainingConcept: TrainingConcept? = null
)

data class TrainingConcept(
    val title: String = "",
    val sections: List<ConceptSection> = emptyList(),
    val pdfUrls: List<String> = emptyList()
)

data class ConceptSection(
    val heading: String = "",
    val body: String = ""
)

// ─── Exercise Database ────────────────────────────────────────────────────────

data class ExerciseTemplate(
    val id: String = "",
    val nameDE: String = "",
    val nameEN: String = "",
    val descriptionDE: String = "",
    val descriptionEN: String = "",
    val photoUrls: List<String> = emptyList(),
    val videoUrls: List<String> = emptyList(),
    val category: String = "",
    val categories: List<String> = emptyList(),
    val searchTerms: List<String> = emptyList(),
    val material: String = "",
    val isTimeBased: Boolean = false,
    val isCustom: Boolean = false,
    val coachId: String = "",
    val organizationId: String = "",
    // "standard" | "reaction_tap" | "circle_overlap" | "color_reaction" | "field_tap" | "color_tap" | "audio_tap" | "pair_find"
    val exerciseType: String = "standard",
    val param2: Int = 0,
    val param3: Int = 0,
    val isDistanceBased: Boolean = false,
    val isInterval: Boolean = false,
    val intervals: List<IntervalConfig> = emptyList(),
    val ratingItems: List<String> = emptyList(),
    val ratingScale: Int = 5
)

data class IntervalConfig(
    val name: String = "",
    val durationSeconds: Int = 0,
    val distanceMeters: Int = 0,
    val repetitions: Int = 1
)

// Mapping sub-category → parent category
val PARENT_CATEGORY = mapOf(
    "Unterschenkel" to "Krafttraining",
    "Oberschenkel" to "Krafttraining",
    "Becken" to "Krafttraining",
    "Bauch" to "Krafttraining",
    "Rücken" to "Krafttraining",
    "Schulter" to "Krafttraining",
    "Brust" to "Krafttraining",
    "Oberarme" to "Krafttraining",
    "Unterarme" to "Krafttraining",
    "Plyometrisches Training" to "Plyometrisches Training",
    "Cardiovaskuläres Training" to "Cardiovaskuläres Training",
    "Mobilisation" to "Mobilisation",
    "Mobilisieren" to "Mobilisation",
    "Koordination" to "Koordination",
    "Reflektion" to "Reflektion"
)

val PARENT_CATEGORIES = listOf(
    "Krafttraining",
    "Plyometrisches Training",
    "Cardiovaskuläres Training",
    "Mobilisation",
    "Koordination",
    "Reflektion"
)

val KRAFTTRAINING_SUBCATEGORIES = listOf(
    "Unterschenkel", "Oberschenkel", "Becken", "Bauch", "Rücken", "Brust", "Oberarme", "Unterarme"
)

fun categoryOrder(category: String): Int {
    val parent = PARENT_CATEGORY[category] ?: category
    val parentIdx = PARENT_CATEGORIES.indexOf(parent).let { if (it < 0) 99 else it }
    val subIdx = if (parent == "Krafttraining")
        KRAFTTRAINING_SUBCATEGORIES.indexOf(category).let { if (it < 0) 50 else it }
    else 0
    return parentIdx * 100 + subIdx
}

// ─── Training Plan ────────────────────────────────────────────────────────────

data class TrainingPlan(
    val id: String = "",
    val coachId: String = "",
    val name: String = "",
    val description: String = "",
    val workoutUnits: List<WorkoutUnit> = emptyList(),
    val sharedWithAthleteIds: List<String> = emptyList(),
    val sharedWithTeamIds: List<String> = emptyList(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

data class WorkoutUnit(
    val id: String = "",
    val planId: String = "",
    val name: String = "",
    val exercises: List<PlannedExercise> = emptyList(),
    val scheduledWeekdays: List<Int> = emptyList(),
    val rhythmDays: Int = 0,
    val startDate: Timestamp? = null,
    val athleteChoosesStartDate: Boolean = false,
    val athleteChoosesDay: Boolean = false,
    val minRestDays: Int = 0,
    val athleteChoosesOrder: Boolean = false,
    val allowShareWorkout: Boolean = true,
    val allowShareProgress: Boolean = true,
    val athleteChosenWeekday: Map<String, Int> = emptyMap(),
    val circuit: Boolean = false,
    val circuitRounds: Int = 3,
    val circuitRestBetweenExercises: Int = 30,  // seconds
    val circuitRestBetweenRounds: Int = 60      // seconds
)

data class AlternativeExercise(
    val id: String = "",
    val templateId: String = "",
    val customName: String = "",
    val customDescription: String = "",
    val customPhotoUrls: List<String> = emptyList(),
    val customVideoUrls: List<String> = emptyList()
)

data class PlannedExercise(
    val id: String = "",
    val templateId: String = "",
    val customName: String = "",
    val customDescription: String = "",
    val customPhotoUrls: List<String> = emptyList(),
    val customVideoUrls: List<String> = emptyList(),
    val sets: Int = 3,
    val reps: Int = 10,
    val durationSeconds: Int = 0,  // 0 = reps based
    val restSeconds: Int = 60,
    val trackWeight: Boolean = false,
    val trackReps: Boolean = false,
    val trackDistance: Boolean = false,
    val trackTempo: Boolean = false,
    val ratingItems: List<String> = emptyList(),
    val ratingScale: Int = 5,
    val orderIndex: Int = 0,
    val alternativeExercises: List<AlternativeExercise> = emptyList(),
    val circuitGroupId: String = "",
    val exerciseType: String = "standard",
    // game-specific settings: "roundSeconds", "circleCount", "overlapRequired", "avgIntervalSeconds", "colorCount"
    val gameParams: Map<String, Int> = emptyMap()
)

data class ExercisePreference(
    val id: String = "",        // "${athleteId}_${exerciseId}"
    val athleteId: String = "",
    val exerciseId: String = "",
    val activeIndex: Int = -1   // -1 = original, 0+ = alternative index
)

// ─── Team ─────────────────────────────────────────────────────────────────────

data class Team(
    val id: String = "",
    val coachId: String = "",
    val name: String = "",
    val athleteIds: List<String> = emptyList()
)

// ─── Workout Session ──────────────────────────────────────────────────────────

data class WorkoutSession(
    val id: String = "",
    val athleteId: String = "",
    val coachId: String = "",
    val planId: String = "",
    val workoutUnitId: String = "",
    val workoutUnitName: String = "",
    val date: Timestamp = Timestamp.now(),
    val completedAt: Timestamp? = null,
    val exerciseEntries: List<ExerciseEntry> = emptyList(),
    val sharedWithCoach: Boolean = false,
    val durationMinutes: Int = 0
)

data class ExerciseEntry(
    val exerciseId: String = "",
    val exerciseName: String = "",
    val sets: List<SetEntry> = emptyList(),
    val ratings: Map<String, Int> = emptyMap()
)

data class SetEntry(
    val setNumber: Int = 0,
    val weight: Float = 0f,
    val reps: Int = 0,
    val durationSeconds: Int = 0,
    val distanceMeters: Float = 0f,
    val tempoKmh: Float = 0f,
    val completed: Boolean = false
)

// ─── Progress ─────────────────────────────────────────────────────────────────

data class ProgressEntry(
    val id: String = "",
    val athleteId: String = "",
    val exerciseId: String = "",
    val exerciseName: String = "",
    val date: Timestamp = Timestamp.now(),
    val maxWeight: Float = 0f,
    val totalReps: Int = 0,
    val totalDurationSeconds: Int = 0,
    val totalDistanceMeters: Float = 0f,
    val avgTempoKmh: Float = 0f,
    // game exercise results
    val avgReactionMs: Long = 0,
    val correctAttempts: Int = 0,
    val wrongAttempts: Int = 0
)

// ─── Chat ─────────────────────────────────────────────────────────────────────

data class ChatConversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    val name: String = "",
    val isGroup: Boolean = false,
    val lastMessage: String = "",
    val lastMessageAt: Timestamp = Timestamp.now(),
    val unreadCount: Map<String, Int> = emptyMap()
)

data class ChatMessage(
    val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false,
    val edited: Boolean = false,
    val pinned: Boolean = false
)

// ─── Athlete Weight Memory ─────────────────────────────────────────────────────

// ─── Training Notes (athlete-private) ─────────────────────────────────────────

data class TrainingNote(
    val id: String = "",
    val athleteId: String = "",
    val text: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

data class LastUsedWeight(
    val athleteId: String = "",
    val exerciseId: String = "",
    val weight: Float = 0f,
    val unit: WeightUnit = WeightUnit.KG,
    val updatedAt: Timestamp = Timestamp.now()
)
