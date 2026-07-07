package com.practicedyad.app.data.repository

import com.practicedyad.app.data.model.*
import com.practicedyad.app.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val firebase: FirebaseService
) {
    val currentUserId: String get() = firebase.currentUserId

    // Auth
    suspend fun register(email: String, password: String) = firebase.register(email, password)
    suspend fun login(email: String, password: String) = firebase.login(email, password)
    fun logout() = firebase.logout()

    // User
    suspend fun createUser(user: AppUser) = firebase.createUser(user)
    suspend fun getUser(uid: String) = firebase.getUser(uid)
    fun getUserFlow(uid: String) = firebase.getUserFlow(uid)
    suspend fun updateUser(uid: String, updates: Map<String, Any>) = firebase.updateUser(uid, updates)
    suspend fun getUserByCode(code: String) = firebase.getUserByCode(code)
    suspend fun connectAthleteToCoach(athleteId: String, coachId: String) =
        firebase.connectAthleteToCoach(athleteId, coachId)
    fun getAthletes(coachId: String) = firebase.getAthletes(coachId)

    // Teams
    suspend fun saveTeam(team: Team) = firebase.saveTeam(team)
    suspend fun deleteTeam(teamId: String) = firebase.deleteTeam(teamId)
    fun getTeams(coachId: String) = firebase.getTeams(coachId)

    // Training Plans
    suspend fun saveTrainingPlan(plan: TrainingPlan) = firebase.saveTrainingPlan(plan)
    suspend fun deleteTrainingPlan(planId: String) = firebase.deleteTrainingPlan(planId)
    fun getCoachPlans(coachId: String) = firebase.getCoachPlans(coachId)
    fun getAthletePlans(athleteId: String) = firebase.getAthletePlans(athleteId)
    suspend fun sharePlanWithAthletes(planId: String, athleteIds: List<String>, teamIds: List<String>) =
        firebase.sharePlanWithAthletes(planId, athleteIds, teamIds)

    // Workout Sessions
    suspend fun saveWorkoutSession(session: WorkoutSession) = firebase.saveWorkoutSession(session)
    fun getAthleteSessions(athleteId: String) = firebase.getAthleteSessions(athleteId)
    fun getSharedSessions(coachId: String, athleteId: String) = firebase.getSharedSessions(coachId, athleteId)

    // Progress
    suspend fun saveProgress(entry: ProgressEntry) = firebase.saveProgress(entry)
    fun getAthleteProgress(athleteId: String) = firebase.getAthleteProgress(athleteId)

    // Weights
    suspend fun saveLastWeight(entry: LastUsedWeight) = firebase.saveLastWeight(entry)
    suspend fun getLastWeight(athleteId: String, exerciseId: String) =
        firebase.getLastWeight(athleteId, exerciseId)

    // Exercises
    fun getExerciseTemplates(coachId: String) = firebase.getExerciseTemplates(coachId)
    fun getStandardExercises() = firebase.getStandardExercises()
    suspend fun saveExerciseTemplate(template: ExerciseTemplate) = firebase.saveExerciseTemplate(template)

    // Chat
    fun getConversations(userId: String) = firebase.getConversations(userId)
    fun getMessages(conversationId: String) = firebase.getMessages(conversationId)
    suspend fun sendMessage(conversationId: String, message: ChatMessage) =
        firebase.sendMessage(conversationId, message)
    suspend fun getOrCreateConversation(userId1: String, userId2: String) =
        firebase.getOrCreateConversation(userId1, userId2)
    suspend fun createGroupConversation(name: String, memberIds: List<String>) =
        firebase.createGroupConversation(name, memberIds)
    suspend fun editMessage(conversationId: String, messageId: String, newContent: String) =
        firebase.editMessage(conversationId, messageId, newContent)
    suspend fun pinMessage(conversationId: String, messageId: String, pinned: Boolean) =
        firebase.pinMessage(conversationId, messageId, pinned)
    suspend fun saveExercisePreference(pref: com.practicedyad.app.data.model.ExercisePreference) =
        firebase.saveExercisePreference(pref)
    fun getExercisePreferences(athleteId: String) = firebase.getExercisePreferences(athleteId)

    // Storage
    suspend fun uploadImage(bytes: ByteArray, path: String) = firebase.uploadImage(bytes, path)

    // Organization
    suspend fun getOrganization(orgId: String) = firebase.getOrganization(orgId)
    suspend fun saveOrganization(org: Organization) = firebase.saveOrganization(org)
    fun getOrganizationFlow(orgId: String) = firebase.getOrganizationFlow(orgId)
    suspend fun getOrgByCode(code: String) = firebase.getOrgByCode(code)

    fun generateConnectCode(): String =
        (1..8).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")

    // Training Notes
    fun getTrainingNotes(athleteId: String) = firebase.getTrainingNotes(athleteId)
    suspend fun addTrainingNote(note: TrainingNote) = firebase.addTrainingNote(note)
    suspend fun deleteTrainingNote(noteId: String) = firebase.deleteTrainingNote(noteId)
}
