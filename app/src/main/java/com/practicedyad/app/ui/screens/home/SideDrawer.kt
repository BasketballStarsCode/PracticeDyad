package com.practicedyad.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import com.practicedyad.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.practicedyad.app.data.model.AppUser
import com.practicedyad.app.data.model.UserRole
import com.practicedyad.app.ui.components.AvatarCircle
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.viewmodel.AuthViewModel

@Composable
fun SideDrawer(
    user: AppUser?,
    navController: NavController,
    onClose: () -> Unit,
    authVm: AuthViewModel = hiltViewModel()
) {
    val isCoach = user?.role == UserRole.COACH || user?.role == UserRole.BOTH
    val isAthlete = user?.role == UserRole.ATHLETE || user?.role == UserRole.BOTH
    val isOrg = user?.organizationId?.isNotEmpty() == true
    val s = LocalAppStrings.current

    ModalDrawerSheet(
        modifier = Modifier.width(280.dp),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TealPrimary)
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AsyncImage(
                        model = R.mipmap.ic_launcher,
                        contentDescription = "PracticeDyad",
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "PracticeDyad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    AvatarCircle(name = user?.name ?: "?", size = 40)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Navigation Items
            DrawerItem(Icons.Default.Person, s.navProfile) {
                onClose(); navController.navigate(Screen.Profile.route)
            }
            DrawerItem(Icons.Default.Description, s.navTrainingPlans) {
                onClose(); navController.navigate(Screen.TrainingPlans.route)
            }

            if (isCoach) {
                DrawerItem(Icons.Default.FitnessCenter, s.navMyExercises) {
                    onClose(); navController.navigate(Screen.MyExercises.route)
                }
                DrawerItem(Icons.Default.Group, s.navAthletes) {
                    onClose(); navController.navigate(Screen.Athletes.route)
                }
            }

            if (isAthlete) {
                DrawerItem(Icons.Default.Edit, s.navTrainingNotes) {
                    onClose(); navController.navigate(Screen.TrainingNotes.route)
                }
            }

            DrawerItem(Icons.Default.TrendingUp, s.navProgress) {
                onClose(); navController.navigate(Screen.Progress.createRoute())
            }
            DrawerItem(Icons.Default.Chat, s.navChat) {
                onClose(); navController.navigate(Screen.ChatList.route)
            }

            if (isOrg) {
                DrawerItem(Icons.Default.Article, s.navTrainingConcept) {
                    onClose(); navController.navigate(Screen.TrainingConcept.route)
                }
                DrawerItem(Icons.Default.Business, s.navOrganization) {
                    onClose(); navController.navigate(Screen.Organization.route)
                }
            }

            DrawerItem(Icons.Default.Settings, s.navSettings) {
                onClose(); navController.navigate(Screen.Settings.route)
            }

            Spacer(Modifier.weight(1f))

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))

            DrawerItem(Icons.Default.Logout, s.navLogout, isDestructive = true) {
                authVm.logout()
                navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, label,
            tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
