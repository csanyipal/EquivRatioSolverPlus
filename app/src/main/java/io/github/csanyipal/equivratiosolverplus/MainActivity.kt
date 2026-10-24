// Copyright 2026 Csányi Pál
//
// This file is part of EquivRatioSolverPlus.
//
// EquivRatioSolverPlus is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
//
// EquivRatioSolverPlus is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along with EquivRatioSolverPlus. If not, see <https://www.gnu.org/licenses/>.

package io.github.csanyipal.equivratiosolverplus

import android.os.Bundle
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.csanyipal.equivratiosolverplus.ui.screens.MainScreen
import io.github.csanyipal.equivratiosolverplus.ui.theme.AppTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val languageTag = newBase.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .getString(LANGUAGE_KEY, null)
        if (languageTag == null) {
            super.attachBaseContext(newBase)
        } else {
            val locale = Locale.forLanguageTag(languageTag)
            Locale.setDefault(locale)
            val configuration = Configuration(newBase.resources.configuration)
            configuration.setLocales(LocaleList(locale))
            super.attachBaseContext(newBase.createConfigurationContext(configuration))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }

    fun setAppLanguage(languageTag: String) {
        val saved = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .putString(LANGUAGE_KEY, languageTag)
            .commit()
        check(saved) { "Could not persist the selected application language." }
        recreate()
    }

    private companion object {
        const val PREFERENCES_NAME = "app_preferences"
        const val LANGUAGE_KEY = "language"
    }
}
