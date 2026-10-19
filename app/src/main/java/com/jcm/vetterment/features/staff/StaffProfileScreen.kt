package com.jcm.vetterment.features.staff

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.supabase.GetProfileData
import com.jcm.vetterment.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun StaffProfileScreen(
    navController: NavController,       // staff nav — for bottom bar
    rootNavController: NavController    // root nav — for logout → login
) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSigningOut by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = ""
        try {
            profile = GetProfileData.getCurrentProfile()
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load profile."
        } finally {
            isLoading = false
        }
    }

    StaffProfileContent(
        navController = navController,
        profile = profile,
        isLoading = isLoading,
        isSigningOut = isSigningOut,
        errorMessage = errorMessage,
        onSignOutClick = { showSignOutDialog = true }
    )

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        isSigningOut = true
                        scope.launch {
                            try {
                                SupabaseClient.client.auth.signOut()
                                rootNavController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                errorMessage = "Sign out failed. Please try again."
                                isSigningOut = false
                            }
                        }
                    }
                ) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun StaffProfileContent(
    navController: NavController,
    profile: Profile?,
    isLoading: Boolean,
    isSigningOut: Boolean,
    errorMessage: String,
    onSignOutClick: () -> Unit
) {
    StaffScaffold(navController = navController) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                errorMessage.isNotBlank() && profile == null -> {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }

                profile != null -> {
                    ProfileBody(
                        profile = profile,
                        isSigningOut = isSigningOut,
                        errorMessage = errorMessage,
                        onSignOutClick = onSignOutClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBody(
    profile: Profile,
    isSigningOut: Boolean,
    errorMessage: String,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text("Profile", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(8.dp))

        // ── Avatar placeholder ────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = profile.full_name
                            .trim()
                            .split(" ")
                            .take(2)
                            .joinToString("") { it.first().uppercase() },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        HorizontalDivider()

        // ── Account info ──────────────────────────────────────────────────
        ProfileSection(title = "Account Information") {
            ProfileRow(label = "Name", value = profile.full_name)
            ProfileRow(label = "Email", value = profile.email)
            ProfileRow(
                label = "Role",
                value = profile.role
                    .replace("_", " ")
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
            )
        }

        HorizontalDivider()

        // ── Error message (e.g. sign out failed) ──────────────────────────
        if (errorMessage.isNotBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ── Sign out ──────────────────────────────────────────────────────
        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSigningOut,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
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
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
}
