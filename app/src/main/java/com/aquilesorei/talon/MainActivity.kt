package com.aquilesorei.talon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.aquilesorei.talon.ui.theme.TalonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TalonTheme {
                val context = LocalContext.current
                val database = remember { TalonDatabase.getDatabase(context) }
                val preferencesRepo = remember { UserPreferencesRepository(database.userPreferencesDao(), context) }
                val preferences by preferencesRepo.preferences.collectAsState(initial = UserPreferences())
                
                if (!preferences.hasCompletedOnboarding) {
                    OnboardingScreen(onComplete = {})
                } else {
                    TalonApp()
                }
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun TalonApp() {
    // État pour gérer l'onglet sélectionné
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            // C'est ici que la navigation opère
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentDestination) {
                    AppDestinations.HOME -> {
                        // Affiche ton écran de scanner Bluetooth
                        HomeScreen()
                    }
                    AppDestinations.GOALS -> {
                        GoalsScreen()
                    }
                    AppDestinations.CHARTS -> {
                        ChartsScreen()
                    }
                    AppDestinations.HISTORY -> {
                        // Placeholder pour l'instant
                        HistoryScreen()
                    }
                    AppDestinations.PROFILE -> {
                        // Placeholder pour l'instant
                       ProfileScreen()
                    }
                    AppDestinations.SETTINGS -> {
                        SettingsScreen()
                    }
                    AppDestinations.HELP -> {
                        HelpScreen()
                    }
                }
            }
        }
    }
}

// Enumération des onglets
enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Measure", Icons.Default.Home),
    GOALS("Goals", Icons.Default.Flag),
    CHARTS("Charts", Icons.Default.TrendingUp),
    HISTORY("History", Icons.Default.History),
    PROFILE("Profile", Icons.Default.AccountBox),
    SETTINGS("Settings", Icons.Default.Settings),
    HELP("Help", Icons.Default.Help),
}

// Un petit composable pour les écrans vides (Profile / History)
@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}