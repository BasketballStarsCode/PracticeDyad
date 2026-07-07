package com.practicedyad.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.*
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.viewmodel.AuthViewModel
import com.practicedyad.app.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    vm: SettingsViewModel = hiltViewModel(),
    authVm: AuthViewModel = hiltViewModel()
) {
    val darkMode by vm.darkMode.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val weightUnit by vm.weightUnit.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val s = LocalAppStrings.current
    val isEN = language == AppLanguage.ENGLISH

    LaunchedEffect(Unit) { vm.loadUser() }

    Scaffold(
        topBar = { PDTopBar(s.settings, onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Appearance
            PDSectionHeader(s.appearance)
            PDCard {
                PDToggle(checked = darkMode, onCheckedChange = { vm.setDarkMode(it) }, label = s.darkMode)
                PDDivider()
                Text(s.language, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip("Deutsch", language == AppLanguage.GERMAN) { vm.setLanguage(AppLanguage.GERMAN) }
                    PDChip("English", language == AppLanguage.ENGLISH) { vm.setLanguage(AppLanguage.ENGLISH) }
                }
                PDDivider()
                Text(s.weightUnit, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip("kg", weightUnit == WeightUnit.KG) { vm.setWeightUnit(WeightUnit.KG) }
                    PDChip("lbs", weightUnit == WeightUnit.LBS) { vm.setWeightUnit(WeightUnit.LBS) }
                }
            }

            // Role
            PDSectionHeader(s.myRole)
            PDCard {
                Text(if (isEN) "What is your role in the app?" else "Als was bist du in der App aktiv?",
                    style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PDChip(s.roleCoach, user?.role == UserRole.COACH) { vm.updateRole(UserRole.COACH) }
                    PDChip(s.roleAthlete, user?.role == UserRole.ATHLETE) { vm.updateRole(UserRole.ATHLETE) }
                    PDChip(s.roleBoth, user?.role == UserRole.BOTH) { vm.updateRole(UserRole.BOTH) }
                }
            }

            // Notifications
            PDSectionHeader(s.notifications)
            PDCard {
                PDToggle(
                    checked = user?.notifyWorkout == true,
                    onCheckedChange = { vm.updateNotifications(notifyWorkout = it) },
                    label = if (isEN) "Today's workout" else "Heutiges Training"
                )
                PDDivider()
                PDToggle(
                    checked = user?.notifyMessages == true,
                    onCheckedChange = { vm.updateNotifications(notifyMessages = it) },
                    label = if (isEN) "New messages" else "Neue Nachrichten"
                )
                PDDivider()
                if (user?.role == UserRole.COACH || user?.role == UserRole.BOTH) {
                    PDToggle(
                        checked = user?.notifyCompletedWorkouts == true,
                        onCheckedChange = { vm.updateNotifications(notifyCompletedWorkouts = it) },
                        label = if (isEN) "Completed workouts" else "Absolvierte Workouts"
                    )
                    PDDivider()
                }
                if (user?.role == UserRole.ATHLETE || user?.role == UserRole.BOTH) {
                    PDToggle(
                        checked = user?.notifyNewPlan == true,
                        onCheckedChange = { vm.updateNotifications(notifyNewPlan = it) },
                        label = if (isEN) "New training plans" else "Neue Trainingspläne"
                    )
                }
            }

            // Privacy
            PDSectionHeader(s.privacy)
            PDCard {
                PDToggle(
                    checked = user?.shareProgressWithCoach == true,
                    onCheckedChange = { vm.updateShareProgress(it) },
                    label = s.shareProgressWithCoach
                )
            }

            // Subscription
            PDSectionHeader(s.subscription)
            PDCard {
                val tierName = when (user?.subscriptionTier) {
                    SubscriptionTier.FREE -> if (isEN) "Free (3 athletes)" else "Kostenlos (3 Athlet*innen)"
                    SubscriptionTier.STANDARD -> if (isEN) "Standard (25 athletes)" else "Standard (25 Athlet*innen)"
                    SubscriptionTier.PROFESSIONAL -> if (isEN) "Professional (60 athletes)" else "Professional (60 Athlet*innen)"
                    SubscriptionTier.UNLIMITED -> "Unlimited"
                    null -> "–"
                }
                Text((if (isEN) "Current plan: " else "Aktueller Tarif: ") + tierName,
                    style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                PDButton(if (isEN) "Manage plan" else "Tarif verwalten",
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(), variant = ButtonVariant.SECONDARY)
            }

            // Logout
            Spacer(Modifier.height(8.dp))
            PDButton(
                text = s.signOut,
                onClick = {
                    vm.logout(authVm)
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.DANGER
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
