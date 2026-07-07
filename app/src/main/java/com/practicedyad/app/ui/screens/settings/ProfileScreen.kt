package com.practicedyad.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.theme.TealPrimary
import com.practicedyad.app.viewmodel.SettingsViewModel

@Composable
fun ProfileScreen(
    navController: NavController,
    vm: SettingsViewModel = hiltViewModel()
) {
    val user by vm.user.collectAsStateWithLifecycle()
    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    LaunchedEffect(user) {
        if (nameInput.isEmpty()) nameInput = user?.name ?: ""
    }
    LaunchedEffect(Unit) { vm.loadUser() }

    Scaffold(
        topBar = { PDTopBar("Profil", onBack = { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Profile photo
            Box(contentAlignment = Alignment.BottomEnd) {
                if (user?.photoUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = user!!.photoUrl,
                        contentDescription = "Profilbild",
                        modifier = Modifier.size(100.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(TealPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.name ?: "?").take(1).uppercase(),
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Camera icon (placeholder — Storage not yet wired up)
                SmallFloatingActionButton(
                    onClick = { /* TODO: Bildauswahl wenn Storage eingerichtet */ },
                    containerColor = TealPrimary,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, "Foto ändern",
                        tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            // Name
            PDCard {
                PDSectionHeader("Angezeigter Name")
                if (editingName) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            label = { Text("Name") }
                        )
                        IconButton(onClick = {
                            vm.updateName(nameInput)
                            editingName = false
                        }) {
                            Icon(Icons.Default.Check, "Speichern", tint = TealPrimary)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(user?.name ?: "–", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        IconButton(onClick = {
                            nameInput = user?.name ?: ""
                            editingName = true
                        }) {
                            Icon(Icons.Default.Edit, "Bearbeiten", tint = TealPrimary)
                        }
                    }
                }
            }

            // Email (read-only)
            PDCard {
                PDSectionHeader("E-Mail-Adresse")
                Text(
                    user?.email ?: "–",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Zum Ändern wende dich an den Support.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
