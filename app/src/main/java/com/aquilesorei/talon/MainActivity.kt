package com.aquilesorei.talon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
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
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalonApp() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    // Split destinations
    val bottomDestinations = listOf(
        AppDestinations.HOME,
        AppDestinations.GOALS,
        AppDestinations.CHARTS,
        AppDestinations.HISTORY
    )
    
    val drawerDestinations = listOf(
        AppDestinations.PROFILE,
        AppDestinations.SETTINGS,
        AppDestinations.HELP
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Talon",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                
                drawerDestinations.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = currentDestination == item,
                        onClick = {
                            currentDestination = item
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                bottomDestinations.forEach { item ->
                    item(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination == item,
                        onClick = { currentDestination = item }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    val openDrawer: () -> Unit = { 
                        scope.launch { drawerState.open() } 
                    }
                    
                    when (currentDestination) {
                        AppDestinations.HOME -> HomeScreen(onOpenDrawer = openDrawer)
                        AppDestinations.GOALS -> GoalsScreen(onOpenDrawer = openDrawer)
                        AppDestinations.CHARTS -> ChartsScreen(onOpenDrawer = openDrawer)
                        AppDestinations.HISTORY -> HistoryScreen(onOpenDrawer = openDrawer)
                        AppDestinations.PROFILE -> ProfileScreen(onOpenDrawer = openDrawer)
                        AppDestinations.SETTINGS -> SettingsScreen(onOpenDrawer = openDrawer)
                        AppDestinations.HELP -> HelpScreen(onOpenDrawer = openDrawer)
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