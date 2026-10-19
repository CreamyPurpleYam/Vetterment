package com.jcm.vetterment.features.branchmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.navigation.BranchManagerRoutes
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun StaffListScreen(navController: NavController) {

    var staff by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = ""
        try {
            staff = GetProfileData.getAllStaff()
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load staff."
        } finally {
            isLoading = false
        }
    }

    StaffListContent(
        navController = navController,
        staff = staff,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onCreateClick = {
            navController.navigate(BranchManagerRoutes.CreateStaffAccount)
        }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun StaffListContent(
    navController: NavController,
    staff: List<Profile>,
    isLoading: Boolean,
    errorMessage: String,
    onCreateClick: () -> Unit
) {
    val vets         = staff.filter { it.role == "vet" }
    val assistants   = staff.filter { it.role == "assistant_vet" }
    val nurses       = staff.filter { it.role == "nurse" }
    val managers     = staff.filter { it.role == "branch_manager" }

    BranchManagerScaffold(
        navController = navController,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "Create staff account")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text("Staff", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@BranchManagerScaffold
            }

            if (staff.isEmpty()) {
                Text(
                    "No staff accounts yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@BranchManagerScaffold
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (managers.isNotEmpty()) {
                    item { StaffSectionHeader("Branch Manager", managers.size) }
                    items(managers) { profile ->
                        StaffCard(profile = profile)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                if (vets.isNotEmpty()) {
                    item { StaffSectionHeader("Veterinarians", vets.size) }
                    items(vets) { profile ->
                        StaffCard(profile = profile)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                if (assistants.isNotEmpty()) {
                    item { StaffSectionHeader("Assistant Veterinarians", assistants.size) }
                    items(assistants) { profile ->
                        StaffCard(profile = profile)
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                if (nurses.isNotEmpty()) {
                    item { StaffSectionHeader("Nurses", nurses.size) }
                    items(nurses) { profile ->
                        StaffCard(profile = profile)
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StaffCard(profile: Profile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initials avatar
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = profile.full_name
                            .trim()
                            .split(" ")
                            .take(2)
                            .joinToString("") { it.first().uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.full_name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = profile.role
                        .replace("_", " ")
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
