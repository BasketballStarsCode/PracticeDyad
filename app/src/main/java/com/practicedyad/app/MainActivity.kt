package com.practicedyad.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.practicedyad.app.ui.theme.LocalAppLanguage
import com.practicedyad.app.ui.theme.LocalAppStrings
import com.practicedyad.app.ui.theme.appStringsFor
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.practicedyad.app.data.model.AppLanguage
import com.practicedyad.app.ui.navigation.NavGraph
import com.practicedyad.app.ui.navigation.Screen
import com.practicedyad.app.ui.theme.PracticeDyadTheme
import com.practicedyad.app.viewmodel.AuthViewModel
import com.practicedyad.app.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val darkMode by settingsVm.darkMode.collectAsStateWithLifecycle()
            val authVm: AuthViewModel = hiltViewModel()

            // Apply locale when language setting changes
            val language by settingsVm.language.collectAsStateWithLifecycle()
            LaunchedEffect(language) {
                val tag = if (language == AppLanguage.ENGLISH) "en" else "de"
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }

            PracticeDyadTheme(darkTheme = darkMode) {
                CompositionLocalProvider(
                    LocalAppLanguage provides language,
                    LocalAppStrings provides appStringsFor(language)
                ) {
                    val navController = rememberNavController()

                    val startDestination = if (authVm.isLoggedIn) {
                        Screen.Home.route
                    } else {
                        Screen.RoleSelection.route
                    }

                    NavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
