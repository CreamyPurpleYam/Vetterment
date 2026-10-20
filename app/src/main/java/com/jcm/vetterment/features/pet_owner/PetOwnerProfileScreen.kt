package com.jcm.vetterment.features.pet_owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.supabase.GetProfileData
import com.jcm.vetterment.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun PetOwnerProfileScreen(navController: NavController) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var isSigningOut by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            profile = GetProfileData.getCurrentProfile()
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load profile."
        } finally {
            isLoading = false
        }
    }

    PetOwnerProfileContent(
        profile = profile,
        isLoading = isLoading,
        errorMessage = errorMessage,
        isSigningOut = isSigningOut,
        navController = navController,
        onSignOutClick = { showSignOutDialog = true }
    )

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    isSigningOut = true
                    coroutineScope.launch {
                        try {
                            SupabaseClient.client.auth.signOut()
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            errorMessage = "Sign out failed. Please try again."
                        } finally {
                            isSigningOut = false
                        }
                    }
                }) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun PetOwnerProfileContent(
    profile: Profile?,
    isLoading: Boolean,
    errorMessage: String,
    isSigningOut: Boolean,
    navController: NavController,
    onSignOutClick: () -> Unit
) {
    PetOwnerScaffold(navController = navController) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Profile", style = MaterialTheme.typography.headlineMedium)

                    if (errorMessage.isNotBlank()) {
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    }

                    if (profile != null) {
                        // ── Profile card ──────────────────────────────────
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = profile.full_name,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = profile.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = profile.role
                                        .lowercase()
                                        .replace("_", " ")
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        profile.created_at?.let { createdAt ->
                            Text(
                                text = "Member since ${createdAt.take(10)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Sign out ──────────────────────────────────────────
                    OutlinedButton(
                        onClick = onSignOutClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSigningOut
                    ) {
                        if (isSigningOut) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Signing out…")
                        } else {
                            Text("Sign Out")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
