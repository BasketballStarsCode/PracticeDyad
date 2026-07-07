package com.practicedyad.app.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.practicedyad.app.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor() {

    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    val currentUserId: String get() = auth.currentUser?.uid ?: ""

    // ─── Auth ─────────────────────────────────────────────────────────────────

    suspend fun register(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: error("Registration failed")
    }

    suspend fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    fun logout() = auth.signOut()

    // ─── User ─────────────────────────────────────────────────────────────────

    suspend fun createUser(user: AppUser) {
        db.collection("users").document(user.id).set(user).await()
    }

    suspend fun getUser(uid: String): AppUser? {
        val doc = db.collection("users").document(uid).get().await()
        return doc.toObject(AppUser::class.java)
    }

    fun getUserFlow(uid: String): Flow<AppUser?> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snap, _ -> trySend(snap?.toObject(AppUser::class.java)) }
        awaitClose { listener.remove() }
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>) {
        db.collection("users").document(uid).update(updates).await()
    }

    suspend fun getUserByCode(code: String): AppUser? {
        val snap = db.collection("users")
            .whereEqualTo("connectCode", code).limit(1).get().await()
        return snap.documents.firstOrNull()?.toObject(AppUser::class.java)
    }

    suspend fun connectAthleteToCoach(athleteId: String, coachId: String) {
        db.collection("users").document(coachId)
            .update("athleteIds", com.google.firebase.firestore.FieldValue.arrayUnion(athleteId)).await()
        db.collection("users").document(athleteId)
            .update("coachIds", com.google.firebase.firestore.FieldValue.arrayUnion(coachId)).await()
    }

    fun getAthletes(coachId: String): Flow<List<AppUser>> = callbackFlow {
        val listener = db.collection("users")
            .whereArrayContains("coachIds", coachId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(AppUser::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ─── Teams ────────────────────────────────────────────────────────────────

    suspend fun saveTeam(team: Team): String {
        val id = team.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("teams").document(id).set(team.copy(id = id)).await()
        return id
    }

    suspend fun deleteTeam(teamId: String) {
        db.collection("teams").document(teamId).delete().await()
    }

    fun getTeams(coachId: String): Flow<List<Team>> = callbackFlow {
        val listener = db.collection("teams")
            .whereEqualTo("coachId", coachId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(Team::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ─── Training Plans ───────────────────────────────────────────────────────

    suspend fun saveTrainingPlan(plan: TrainingPlan): String {
        val id = plan.id.ifEmpty { UUID.randomUUID().toString() }
        val updatedPlan = plan.copy(id = id, updatedAt = com.google.firebase.Timestamp.now())
        db.collection("trainingPlans").document(id).set(updatedPlan).await()
        return id
    }

    suspend fun deleteTrainingPlan(planId: String) {
        db.collection("trainingPlans").document(planId).delete().await()
    }

    fun getCoachPlans(coachId: String): Flow<List<TrainingPlan>> = callbackFlow {
        val listener = db.collection("trainingPlans")
            .whereEqualTo("coachId", coachId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(TrainingPlan::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getAthletePlans(athleteId: String): Flow<List<TrainingPlan>> = callbackFlow {
        val listener = db.collection("trainingPlans")
            .whereArrayContains("sharedWithAthleteIds", athleteId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(TrainingPlan::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun sharePlanWithAthletes(planId: String, athleteIds: List<String>, teamIds: List<String>) {
        db.collection("trainingPlans").document(planId).update(
            mapOf(
                "sharedWithAthleteIds" to athleteIds,
                "sharedWithTeamIds" to teamIds
            )
        ).await()
    }

    // ─── Workout Sessions ─────────────────────────────────────────────────────

    suspend fun saveWorkoutSession(session: WorkoutSession): String {
        val id = session.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("workoutSessions").document(id).set(session.copy(id = id)).await()
        return id
    }

    fun getAthleteSessions(athleteId: String): Flow<List<WorkoutSession>> = callbackFlow {
        val listener = db.collection("workoutSessions")
            .whereEqualTo("athleteId", athleteId)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(WorkoutSession::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getSharedSessions(coachId: String, athleteId: String): Flow<List<WorkoutSession>> = callbackFlow {
        val listener = db.collection("workoutSessions")
            .whereEqualTo("athleteId", athleteId)
            .whereEqualTo("coachId", coachId)
            .whereEqualTo("sharedWithCoach", true)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(WorkoutSession::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ─── Progress ─────────────────────────────────────────────────────────────

    suspend fun saveProgress(entry: ProgressEntry): String {
        val id = entry.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("progressEntries").document(id).set(entry.copy(id = id)).await()
        return id
    }

    fun getAthleteProgress(athleteId: String): Flow<List<ProgressEntry>> = callbackFlow {
        val listener = db.collection("progressEntries")
            .whereEqualTo("athleteId", athleteId)
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(ProgressEntry::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ─── Last Used Weights ────────────────────────────────────────────────────

    suspend fun saveLastWeight(entry: LastUsedWeight) {
        val docId = "${entry.athleteId}_${entry.exerciseId}"
        db.collection("lastWeights").document(docId).set(entry).await()
    }

    suspend fun getLastWeight(athleteId: String, exerciseId: String): LastUsedWeight? {
        val doc = db.collection("lastWeights")
            .document("${athleteId}_${exerciseId}").get().await()
        return doc.toObject(LastUsedWeight::class.java)
    }

    // ─── Exercise Templates ───────────────────────────────────────────────────

    fun getExerciseTemplates(coachId: String): Flow<List<ExerciseTemplate>> = callbackFlow {
        val listener = db.collection("exerciseTemplates")
            .whereEqualTo("coachId", coachId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(ExerciseTemplate::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getStandardExercises(): Flow<List<ExerciseTemplate>> = callbackFlow {
        // Seed on first use if collection is empty
        try { StandardDatabaseSeeder.seedIfNeeded(db) } catch (_: Exception) {}
        val listener = db.collection("standardExercises")
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(ExerciseTemplate::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveExerciseTemplate(template: ExerciseTemplate): String {
        val id = template.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("exerciseTemplates").document(id).set(template.copy(id = id)).await()
        return id
    }

    // ─── Chat ─────────────────────────────────────────────────────────────────

    fun getConversations(userId: String): Flow<List<ChatConversation>> = callbackFlow {
        val listener = db.collection("conversations")
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(ChatConversation::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    fun getMessages(conversationId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = db.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(ChatMessage::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(conversationId: String, message: ChatMessage) {
        val msgId = UUID.randomUUID().toString()
        db.collection("conversations").document(conversationId)
            .collection("messages").document(msgId)
            .set(message.copy(id = msgId)).await()
        db.collection("conversations").document(conversationId).update(
            mapOf(
                "lastMessage" to message.content,
                "lastMessageAt" to message.timestamp
            )
        ).await()
    }

    suspend fun getOrCreateConversation(userId1: String, userId2: String): String {
        val sorted = listOf(userId1, userId2).sorted()
        val convId = "${sorted[0]}_${sorted[1]}"
        val doc = db.collection("conversations").document(convId).get().await()
        if (!doc.exists()) {
            db.collection("conversations").document(convId).set(
                ChatConversation(
                    id = convId,
                    participantIds = sorted,
                    lastMessage = "",
                    lastMessageAt = com.google.firebase.Timestamp.now()
                )
            ).await()
        }
        return convId
    }

    suspend fun editMessage(conversationId: String, messageId: String, newContent: String) {
        db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
            .update(mapOf("content" to newContent, "edited" to true)).await()
    }

    suspend fun pinMessage(conversationId: String, messageId: String, pinned: Boolean) {
        db.collection("conversations").document(conversationId)
            .collection("messages").document(messageId)
            .update(mapOf("pinned" to pinned)).await()
    }

    suspend fun saveExercisePreference(pref: ExercisePreference) {
        db.collection("exercisePreferences").document(pref.id).set(pref).await()
    }

    fun getExercisePreferences(athleteId: String): Flow<List<ExercisePreference>> = callbackFlow {
        val listener = db.collection("exercisePreferences")
            .whereEqualTo("athleteId", athleteId)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(ExercisePreference::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun createGroupConversation(name: String, memberIds: List<String>): String {
        val convId = java.util.UUID.randomUUID().toString()
        db.collection("conversations").document(convId).set(
            ChatConversation(
                id = convId,
                participantIds = memberIds,
                name = name,
                isGroup = true,
                lastMessage = "",
                lastMessageAt = com.google.firebase.Timestamp.now()
            )
        ).await()
        return convId
    }

    // ─── Storage ──────────────────────────────────────────────────────────────

    suspend fun uploadImage(bytes: ByteArray, path: String): String {
        val ref = storage.reference.child(path)
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }

    // ─── Organization ─────────────────────────────────────────────────────────

    suspend fun getOrganization(orgId: String): Organization? {
        val doc = db.collection("organizations").document(orgId).get().await()
        return doc.toObject(Organization::class.java)
    }

    suspend fun saveOrganization(org: Organization): String {
        val id = org.id.ifEmpty { UUID.randomUUID().toString() }
        db.collection("organizations").document(id).set(org.copy(id = id)).await()
        return id
    }

    fun getOrganizationFlow(orgId: String): Flow<Organization?> = callbackFlow {
        val listener = db.collection("organizations").document(orgId)
            .addSnapshotListener { snap, _ -> trySend(snap?.toObject(Organization::class.java)) }
        awaitClose { listener.remove() }
    }

    suspend fun getOrgByCode(code: String): Organization? {
        val snap = db.collection("organizations")
            .whereEqualTo("accessCode", code).limit(1).get().await()
        return snap.documents.firstOrNull()?.toObject(Organization::class.java)
    }

    // Training Notes (athlete-private, not visible to coaches)
    fun getTrainingNotes(athleteId: String): Flow<List<TrainingNote>> = callbackFlow {
        val listener = db.collection("trainingNotes")
            .whereEqualTo("athleteId", athleteId)
            .addSnapshotListener { snap, _ ->
                val notes = snap?.documents
                    ?.mapNotNull { it.toObject(TrainingNote::class.java)?.copy(id = it.id) }
                    ?.sortedByDescending { it.createdAt.seconds }
                    ?: emptyList()
                trySend(notes)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addTrainingNote(note: TrainingNote) {
        val id = if (note.id.isEmpty()) UUID.randomUUID().toString() else note.id
        db.collection("trainingNotes").document(id).set(note.copy(id = id)).await()
    }

    suspend fun deleteTrainingNote(noteId: String) {
        db.collection("trainingNotes").document(noteId).delete().await()
    }
}
