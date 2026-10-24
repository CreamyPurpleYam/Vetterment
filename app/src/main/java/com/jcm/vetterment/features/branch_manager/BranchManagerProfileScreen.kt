package com.jcm.vetterment.features.branchmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.auth.formatName
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.supabase.GetProfileData
import com.jcm.vetterment.supabase.SupabaseClient
import kotlinx.coroutines.launch

@Composable
fun BranchManagerProfileScreen(navController: NavController) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
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

    val onLogoutClick = { showLogoutDialog = true }
    val onLogoutConfirm = {
        showLogoutDialog = false
        scope.launch {
            try {
                SupabaseClient.client.auth.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            navController.navigate("login") {
            popUpTo("login") { inclusive = true }
            launchSingleTop = true
            restoreState = false
        }
    }

    BranchManagerProfileContent(
        navController = navController,
        profile = profile,
        isLoading = isLoading,
        errorMessage = errorMessage,
        showLogoutDialog = showLogoutDialog,
        onLogoutClick = onLogoutClick,
        onLogoutConfirm = onLogoutConfirm
    )
}

@Composable
fun BranchManagerProfileContent(
    navController: NavController,
    profile: Profile?,
    isLoading: Boolean,
    errorMessage: String,
    showLogoutDialog: Boolean,
    onLogoutClick: () -> Unit,
    onLogoutConfirm: () -> Unit
) {
    BranchManagerScaffold(navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@BranchManagerScaffold
            }

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            profile?.let { p ->
                // Profile header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = p.full_name
                                                .trim()
                                                .split(" ")
                                                .take(2)
                                                .joinToString("") { it.first().uppercase() },
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(p.full_name, style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    text = p.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Role badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Role", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = p.role
                                        .replace("_", " ")
                                        .lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Account actions
                Text("Account", style = MaterialTheme.typography.titleMedium)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuItem(
                        icon = Icons.Default.Person,
                        label = "Edit Profile",
                        onClick = { /* TODO: Navigate to edit profile */ }
                    )
                    MenuItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = { /* TODO: Navigate to settings */ }
                    )
                    MenuItem(
                        icon = Icons.Default.Logout,
                        label = "Log Out",
                        onClick = onLogoutClick,
                        isDestructive = true
                    )
                }
            }
        }
    }

    // Logout dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { /* handled by parent */ },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(onClick = onLogoutConfirm) {
                    Text("Log Out", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.error))
                }
            },
            dismissButton = {
                TextButton(onClick = onLogoutClick) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = Icons.Filled.NavigateNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}