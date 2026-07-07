package com.practicedyad.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.practicedyad.app.data.model.UserRole
import com.practicedyad.app.ui.components.*
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    navController: NavController,
    roleString: String,
    vm: AuthViewModel = hiltViewModel()
) {
    val role = runCatching { UserRole.valueOf(roleString) }.getOrDefault(UserRole.ATHLETE)
    val state by vm.uiState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var orgCode by remember { mutableStateOf("") }

    val roleLabel = when (role) {
        UserRole.COACH -> "Coach"
        UserRole.ATHLETE -> "Athlet*in"
        UserRole.ORGANIZATION -> "Organisation"
        else -> "Nutzer*in"
    }

    LaunchedEffect(state.success) {
        if (state.success) {
            vm.clearSuccess()
            navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
        }
    }

    Scaffold { padding ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 28.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(60.dp))
                Text("Registrieren als", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(roleLabel, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)

                Spacer(Modifier.height(40.dp))

                PDTextField(value = name, onValueChange = { name = it }, label = "Name")
                Spacer(Modifier.height(14.dp))
                PDTextField(value = email, onValueChange = { email = it }, label = "E-Mail",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                Spacer(Modifier.height(14.dp))
                PDTextField(
                    value = password, onValueChange = { password = it }, label = "Passwort",
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    }
                )

                if (role == UserRole.COACH) {
                    Spacer(Modifier.height(14.dp))
                    PDTextField(value = orgCode, onValueChange = { orgCode = it },
                        label = "Organisationscode (optional)",
                        placeholder = "Code eingeben, falls vorhanden")
                }

                state.error?.let { err ->
                    Spacer(Modifier.height(12.dp))
                    Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(32.dp))

                PDButton(
                    text = "Konto erstellen",
                    onClick = { vm.register(email, password, name, role, orgCode) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6 && !state.loading
                )

                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Zurück")
                }
                Spacer(Modifier.height(32.dp))
            }

            if (state.loading) PDLoadingOverlay()
        }
    }
}
