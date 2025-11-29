package com.aquilesorei.talon.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.aquilesorei.talon.viewmodels.HistoryViewModel
import com.aquilesorei.talon.domain.models.UserProfile
import com.aquilesorei.talon.data.local.entities.GoalStatus
import com.aquilesorei.talon.data.local.entities.GoalType
import com.aquilesorei.talon.data.local.entities.Goal
import com.aquilesorei.talon.data.local.entities.Measurement
import com.aquilesorei.talon.data.repository.UserProfileRepository
import com.aquilesorei.talon.data.repository.GoalRepository
import com.aquilesorei.talon.data.repository.MeasurementRepository
import com.aquilesorei.talon.data.local.database.TalonDatabase
import com.aquilesorei.talon.data.repository.UserPreferencesRepository

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { TalonDatabase.getDatabase(context) }
    val preferencesRepo = remember { UserPreferencesRepository(database.userPreferencesDao(), context) }
    
    val pages = listOf(
        OnboardingPage(
            "Welcome to Talon",
            "Your smart scale companion for tracking body composition and achieving your fitness goals",
            Icons.Default.FitnessCenter
        ),
        OnboardingPage(
            "Connect Your Scale",
            "Scan and connect to your Bluetooth smart scale to automatically capture measurements",
            Icons.Default.Bluetooth
        ),
        OnboardingPage(
            "Track Progress",
            "View detailed charts and statistics to monitor your weight, body fat, muscle mass, and more",
            Icons.Default.TrendingUp
        ),
        OnboardingPage(
            "Set Goals",
            "Create personalized goals and track your progress with visual indicators and achievements",
            Icons.Default.Flag
        ),
        OnboardingPage(
            "Export & Share",
            "Export your data to CSV or create backups. Share your progress with healthcare providers",
            Icons.Default.Share
        )
    )
    
    val pagerState = rememberPagerState(pageCount = { pages.size })
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    scope.launch {
                        preferencesRepo.completeOnboarding()
                        onComplete()
                    }
                }
            ) {
                Text("Skip")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(pages[page])
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Page indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(pages.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                        .then(
                            if (pagerState.currentPage == index)
                                Modifier.background(
                                    MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                )
                            else
                                Modifier.background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowBack, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Back")
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            
            Button(
                onClick = {
                    scope.launch {
                        if (pagerState.currentPage < pages.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            preferencesRepo.completeOnboarding()
                            onComplete()
                        }
                    }
                }
            ) {
                Text(if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started")
                if (pagerState.currentPage < pages.size - 1) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null)
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
