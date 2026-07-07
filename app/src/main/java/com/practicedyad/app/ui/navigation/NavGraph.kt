package com.practicedyad.app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.practicedyad.app.ui.screens.athletes.*
import com.practicedyad.app.ui.screens.auth.*
import com.practicedyad.app.ui.screens.chat.*
import com.practicedyad.app.ui.screens.exercises.*
import com.practicedyad.app.ui.screens.home.HomeScreen
import com.practicedyad.app.ui.screens.organization.*
import com.practicedyad.app.ui.screens.progress.ProgressScreen
import com.practicedyad.app.ui.screens.settings.ProfileScreen
import com.practicedyad.app.ui.screens.settings.SettingsScreen
import com.practicedyad.app.ui.screens.trainingplans.*
import com.practicedyad.app.ui.screens.workout.WorkoutExecutionScreen
import com.practicedyad.app.ui.screens.notes.TrainingNotesScreen
import com.practicedyad.app.ui.screens.trainingplans.TrainingPlanPreviewScreen
import com.practicedyad.app.ui.screens.workout.ReactionGamesScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(navController)
        }
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        composable(
            Screen.Register.route,
            arguments = listOf(navArgument("role") { type = NavType.StringType })
        ) { back ->
            RegisterScreen(navController, back.arguments?.getString("role") ?: "ATHLETE")
        }

        composable(Screen.Home.route) {
            HomeScreen(navController)
        }

        composable(Screen.TrainingPlans.route) {
            TrainingPlansScreen(navController)
        }
        composable(
            Screen.CreatePlan.route,
            arguments = listOf(navArgument("planId") { defaultValue = ""; type = NavType.StringType })
        ) { back ->
            CreateTrainingPlanScreen(navController, back.arguments?.getString("planId") ?: "")
        }
        composable(
            Screen.WorkoutExecution.route,
            arguments = listOf(
                navArgument("unitId") { type = NavType.StringType },
                navArgument("planId") { type = NavType.StringType }
            )
        ) { back ->
            WorkoutExecutionScreen(
                navController,
                back.arguments?.getString("unitId") ?: "",
                back.arguments?.getString("planId") ?: ""
            )
        }

        composable(Screen.Athletes.route) {
            AthletesScreen(navController)
        }
        composable(
            Screen.AthleteDetail.route,
            arguments = listOf(navArgument("athleteId") { type = NavType.StringType })
        ) { back ->
            AthleteDetailScreen(navController, back.arguments?.getString("athleteId") ?: "")
        }
        composable(
            Screen.CreateTeam.route,
            arguments = listOf(navArgument("teamId") { defaultValue = ""; type = NavType.StringType })
        ) { back ->
            CreateTeamScreen(navController, back.arguments?.getString("teamId") ?: "")
        }
        composable(
            Screen.SharedWorkouts.route,
            arguments = listOf(navArgument("athleteId") { type = NavType.StringType })
        ) { back ->
            SharedWorkoutsScreen(navController, back.arguments?.getString("athleteId") ?: "")
        }
        composable(
            Screen.SharedWorkoutDetail.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { back ->
            SharedWorkoutDetailScreen(navController, back.arguments?.getString("sessionId") ?: "")
        }

        composable(
            Screen.Progress.route,
            arguments = listOf(navArgument("athleteId") { defaultValue = ""; type = NavType.StringType })
        ) { back ->
            ProgressScreen(navController, back.arguments?.getString("athleteId") ?: "")
        }

        composable(Screen.MyExercises.route) {
            MyExercisesScreen(navController)
        }
        composable(
            Screen.ExerciseEditor.route,
            arguments = listOf(navArgument("templateId") { defaultValue = ""; type = NavType.StringType })
        ) { back ->
            ExerciseEditorScreen(navController, back.arguments?.getString("templateId") ?: "")
        }

        composable(Screen.ChatList.route) {
            ChatListScreen(navController)
        }
        composable(
            Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("otherUserId") { type = NavType.StringType }
            )
        ) { back ->
            ChatScreen(
                navController,
                back.arguments?.getString("conversationId") ?: "",
                back.arguments?.getString("otherUserId") ?: ""
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        composable(Screen.Organization.route) {
            OrganizationScreen(navController)
        }
        composable(Screen.TrainingConcept.route) {
            TrainingConceptScreen(navController)
        }
        composable(Screen.TrainingNotes.route) {
            TrainingNotesScreen(navController)
        }
        composable(
            Screen.PlanPreview.route,
            arguments = listOf(navArgument("planId") { type = NavType.StringType })
        ) { back ->
            TrainingPlanPreviewScreen(navController, back.arguments?.getString("planId") ?: "")
        }

        composable(
            Screen.ReactionGame.route,
            arguments = listOf(
                navArgument("exerciseType") { type = NavType.StringType },
                navArgument("exerciseId") { type = NavType.StringType },
                navArgument("roundSeconds") { type = NavType.IntType },
                navArgument("rounds") { type = NavType.IntType },
                navArgument("param2") { type = NavType.IntType },
                navArgument("param3") { type = NavType.IntType }
            )
        ) { back ->
            ReactionGamesScreen(
                navController = navController,
                exerciseType = back.arguments?.getString("exerciseType") ?: "reaction_tap",
                exerciseId = back.arguments?.getString("exerciseId") ?: "",
                roundSeconds = back.arguments?.getInt("roundSeconds") ?: 60,
                rounds = back.arguments?.getInt("rounds") ?: 3,
                param2 = back.arguments?.getInt("param2") ?: 0,
                param3 = back.arguments?.getInt("param3") ?: 0
            )
        }
    }
}
