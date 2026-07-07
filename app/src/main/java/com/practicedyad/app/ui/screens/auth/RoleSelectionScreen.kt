package com.practicedyad.app.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.practicedyad.app.R
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.TealPrimary

@Composable
fun RoleSelectionScreen(navController: NavController) {
    var showOrgDialog by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo + Name
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // App logo placeholder — replace with actual logo drawable
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(24.dp),
                    color = TealPrimary
                ) {}
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "PracticeDyad",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = TealPrimary
            )
            Text(
                "Dein Trainingspartner",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(56.dp))

            Text(
                "Ich bin …",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(24.dp))

            RoleCard(
                title = "Coach",
                subtitle = "Erstelle und teile Trainingspläne",
                emoji = "🏋️",
                onClick = { navController.navigate(Screen.Register.createRoute("COACH")) }
            )
            Spacer(Modifier.height(16.dp))
            RoleCard(
                title = "Athlet*in",
                subtitle = "Folge deinem Trainingsplan",
                emoji = "🏃",
                onClick = { navController.navigate(Screen.Register.createRoute("ATHLETE")) }
            )
            Spacer(Modifier.height(16.dp))
            RoleCard(
                title = "Organisation",
                subtitle = "Verwalte ein Coach-Team",
                emoji = "🏢",
                onClick = { showOrgDialog = true }
            )

            Spacer(Modifier.height(32.dp))

            TextButton(onClick = { navController.navigate(Screen.Login.route) }) {
                Text("Bereits registriert? Einloggen", color = TealPrimary)
            }
        }
    }

    if (showOrgDialog) {
        AlertDialog(
            onDismissRequest = { showOrgDialog = false },
            title = { Text("Organisation erstellen") },
            text = { Text("Als Organisation verwaltest du ein Team aus Coaches und deren Athlet*innen. Wähle deinen Tarif nach der Registrierung.") },
            confirmButton = {
                Button(onClick = {
                    showOrgDialog = false
                    navController.navigate(Screen.Register.createRoute("ORGANIZATION"))
                }) { Text("Weiter") }
            },
            dismissButton = {
                TextButton(onClick = { showOrgDialog = false }) { Text("Abbrechen") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun RoleCard(title: String, subtitle: String, emoji: String, onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 36.sp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
