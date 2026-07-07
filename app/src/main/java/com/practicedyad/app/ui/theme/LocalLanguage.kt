package com.practicedyad.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.practicedyad.app.data.model.AppLanguage
import com.practicedyad.app.data.model.ExerciseTemplate

val LocalAppLanguage = compositionLocalOf { AppLanguage.GERMAN }

fun ExerciseTemplate.localName(lang: AppLanguage): String =
    if (lang == AppLanguage.ENGLISH && nameEN.isNotBlank()) nameEN else nameDE

fun ExerciseTemplate.localDescription(lang: AppLanguage): String =
    if (lang == AppLanguage.ENGLISH && descriptionEN.isNotBlank()) descriptionEN else descriptionDE
