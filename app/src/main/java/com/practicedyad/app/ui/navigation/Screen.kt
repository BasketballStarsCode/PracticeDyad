package com.practicedyad.app.ui.navigation

sealed class Screen(val route: String) {
    // Auth
    object RoleSelection    : Screen("role_selection")
    object Login            : Screen("login")
    object Register         : Screen("register/{role}") {
        fun createRoute(role: String) = "register/$role"
    }

    // Main
    object Home             : Screen("home")

    // Training Plans
    object TrainingPlans    : Screen("training_plans")
    object CreatePlan       : Screen("create_plan?planId={planId}") {
        fun createRoute(planId: String = "") = "create_plan?planId=$planId"
    }
    object WorkoutExecution : Screen("workout_execution/{unitId}/{planId}") {
        fun createRoute(unitId: String, planId: String) = "workout_execution/$unitId/$planId"
    }

    // Athletes
    object Athletes         : Screen("athletes")
    object AthleteDetail    : Screen("athlete_detail/{athleteId}") {
        fun createRoute(id: String) = "athlete_detail/$id"
    }
    object CreateTeam       : Screen("create_team?teamId={teamId}") {
        fun createRoute(teamId: String = "") = "create_team?teamId=$teamId"
    }
    object SharedWorkouts   : Screen("shared_workouts/{athleteId}") {
        fun createRoute(id: String) = "shared_workouts/$id"
    }
    object SharedWorkoutDetail : Screen("shared_workout_detail/{sessionId}") {
        fun createRoute(id: String) = "shared_workout_detail/$id"
    }

    // Progress
    object Progress         : Screen("progress?athleteId={athleteId}") {
        fun createRoute(athleteId: String = "") = "progress?athleteId=$athleteId"
    }

    // Exercises
    object MyExercises      : Screen("my_exercises")
    object ExerciseEditor   : Screen("exercise_editor?templateId={templateId}") {
        fun createRoute(templateId: String = "") = "exercise_editor?templateId=$templateId"
    }

    // Chat
    object ChatList         : Screen("chat_list")
    object Chat             : Screen("chat/{conversationId}/{otherUserId}") {
        fun createRoute(convId: String, otherId: String) = "chat/$convId/$otherId"
    }

    // Profile & Settings
    object Profile          : Screen("profile")
    object Settings         : Screen("settings")

    // Organization
    object Organization     : Screen("organization")
    object TrainingConcept  : Screen("training_concept")

    // Training Notes (athlete)
    object TrainingNotes     : Screen("training_notes")

    // Training Plan Preview
    object PlanPreview       : Screen("plan_preview/{planId}") {
        fun createRoute(planId: String) = "plan_preview/$planId"
    }

    // Reaction Games
    object ReactionGame : Screen("reaction_game/{exerciseType}/{exerciseId}/{roundSeconds}/{rounds}/{param2}/{param3}") {
        fun createRoute(
            exerciseType: String,
            exerciseId: String,
            roundSeconds: Int,
            rounds: Int,
            param2: Int = 0,
            param3: Int = 0
        ) = "reaction_game/$exerciseType/$exerciseId/$roundSeconds/$rounds/$param2/$param3"
    }
}
