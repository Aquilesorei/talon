package com.aquilesorei.talon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { TalonDatabase.getDatabase(context) }
    val repository = remember { UserProfileRepository(database.userProfileDao()) }

    // On observe les données actuelles depuis la BDD
    val userProfile by repository.userProfile.collectAsState(initial = UserProfile(1, 175, 25, true))

    // États locaux pour les champs de formulaire (éditables)
    var heightInput by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf("") }
    var isMale by remember { mutableStateOf(true) }
    var isAthlete by remember { mutableStateOf(false) }

    // Initialiser les champs quand les données arrivent
    LaunchedEffect(userProfile) {
        if (heightInput.isEmpty()) heightInput = userProfile.heightCm.toString()
        if (ageInput.isEmpty()) ageInput = userProfile.age.toString()
        isMale = userProfile.isMale
        isAthlete = userProfile.isAthlete
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, "Menu")
            }
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Your Profile", style = MaterialTheme.typography.headlineMedium)
        Text("Needed for body fat calculation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(32.dp))

        // --- Champs de saisie ---

        OutlinedTextField(
            value = heightInput,
            onValueChange = { if (it.all { char -> char.isDigit() }) heightInput = it },
            label = { Text("Height (cm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ageInput,
            onValueChange = { if (it.all { char -> char.isDigit() }) ageInput = it },
            label = { Text("Age") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- Switchs ---

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Gender: ${if (isMale) "Male" else "Female"}")
            Switch(checked = isMale, onCheckedChange = { isMale = it })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Athlete Mode")
                Text("More muscle mass", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Switch(checked = isAthlete, onCheckedChange = { isAthlete = it })
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- Bouton Sauvegarder ---

        Button(
            onClick = {
                val h = heightInput.toIntOrNull() ?: 175
                val a = ageInput.toIntOrNull() ?: 25
                scope.launch {
                    repository.saveProfile(h, a, isMale, isAthlete)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Profile")
        }
    }
}