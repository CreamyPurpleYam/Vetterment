package com.jcm.vetterment.features.branchmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jcm.vetterment.features.auth.formatName
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.navigation.BranchManagerRoutes
import com.jcm.vetterment.supabase.GetProfileData
import kotlinx.coroutines.launch

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun StaffListScreen(navController: NavController) {
    var staff by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var currentUserId by remember { mutableStateOf<String?>(null) }

    // Edit dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    var editFullName by remember { mutableStateOf("") }
    var editRole by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var editError by remember { mutableStateOf("") }

    // Delete dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletingProfile by remember { mutableStateOf<Profile?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    fun refreshStaff() {
        scope.launch {
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
    }

    // Load current user ID once
    LaunchedEffect(Unit) {
        val profile = GetProfileData.getCurrentProfile()
        currentUserId = profile?.id
        refreshStaff()
    }

    // Refresh staff list when returning to this screen from CreateStaffScreen
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var lastRoute by remember { mutableStateOf(currentRoute) }

    LaunchedEffect(currentRoute) {
        // If we're on staff list and came from create staff screen, refresh
        if (currentRoute == BranchManagerRoutes.StaffList &&
            lastRoute == BranchManagerRoutes.CreateStaffAccount) {
            refreshStaff()
        }
        lastRoute = currentRoute
    }

    // Edit staff
    LaunchedEffect(isEditing) {
        if (!isEditing || editingProfile == null) return@LaunchedEffect
        try {
            val result = GetProfileData.updateStaffProfile(
                profileId = editingProfile!!.id,
                fullName = editFullName.trim().takeIf { it.isNotBlank() },
                role = editRole.trim().takeIf { it.isNotBlank() }
            )
            if (result.isSuccess) {
                showEditDialog = false
                editError = ""
                refreshStaff()
            } else {
                editError = result.exceptionOrNull()?.message ?: "Failed to update staff."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            editError = "Something went wrong."
        } finally {
            isEditing = false
        }
    }

    // Delete staff
    LaunchedEffect(isDeleting) {
        if (!isDeleting || deletingProfile == null) return@LaunchedEffect
        try {
            val success = GetProfileData.deleteStaffProfile(deletingProfile!!.id)
            if (success) {
                showDeleteDialog = false
                deleteError = ""
                refreshStaff()
            } else {
                deleteError = "Failed to delete staff."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            deleteError = "Something went wrong."
        } finally {
            isDeleting = false
        }
    }

    StaffListContent(
        navController = navController,
        staff = staff,
        isLoading = isLoading,
        errorMessage = errorMessage,
        currentUserId = currentUserId,
        onCreateClick = {
            navController.navigate(BranchManagerRoutes.CreateStaffAccount)
        },
        onEditClick = { profile ->
            editingProfile = profile
            editFullName = profile.full_name
            editRole = profile.role
            showEditDialog = true
        },
        onDeleteClick = { profile ->
            deletingProfile = profile
            showDeleteDialog = true
        },
        showEditDialog = showEditDialog,
        showDeleteDialog = showDeleteDialog,
        editingProfile = editingProfile,
        deletingProfile = deletingProfile,
        editFullName = editFullName,
        editRole = editRole,
        onEditFullNameChange = { editFullName = it; editError = "" },
        onEditRoleChange = { editRole = it; editError = "" },
        isEditing = isEditing,
        isDeleting = isDeleting,
        editError = editError,
        deleteError = deleteError,
        onEditDismiss = { showEditDialog = false; editError = "" },
        onDeleteDismiss = { showDeleteDialog = false; deleteError = "" },
        onEditConfirm = { isEditing = true },
        onDeleteConfirm = { isDeleting = true }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffListContent(
    navController: NavController,
    staff: List<Profile>,
    isLoading: Boolean,
    errorMessage: String,
    currentUserId: String?,
    onCreateClick: () -> Unit,
    onEditClick: (Profile) -> Unit,
    onDeleteClick: (Profile) -> Unit,
    showEditDialog: Boolean,
    showDeleteDialog: Boolean,
    editingProfile: Profile?,
    deletingProfile: Profile?,
    editFullName: String,
    editRole: String,
    onEditFullNameChange: (String) -> Unit,
    onEditRoleChange: (String) -> Unit,
    isEditing: Boolean,
    isDeleting: Boolean,
    editError: String,
    deleteError: String,
    onEditDismiss: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onEditConfirm: () -> Unit,
    onDeleteConfirm: () -> Unit
) {
    val vets         = staff.filter { it.role == "vet" }
    val assistants   = staff.filter { it.role == "assistant_vet" }
    val nurses       = staff.filter { it.role == "nurse" }
    val managers     = staff.filter { it.role == "branch_manager" }

    val roles = listOf(
        "vet" to "Veterinarian",
        "assistant_vet" to "Assistant Veterinarian",
        "nurse" to "Nurse"
    )

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
                        StaffCard(
                            profile = profile,
                            onEditClick = { onEditClick(profile) },
                            onDeleteClick = { onDeleteClick(profile) },
                            isDeletable = profile.id != currentUserId
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                if (vets.isNotEmpty()) {
                    item { StaffSectionHeader("Veterinarians", vets.size) }
                    items(vets) { profile ->
                        StaffCard(
                            profile = profile,
                            onEditClick = { onEditClick(profile) },
                            onDeleteClick = { onDeleteClick(profile) },
                            isDeletable = profile.id != currentUserId
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                if (assistants.isNotEmpty()) {
                    item { StaffSectionHeader("Assistant Veterinarians", assistants.size) }
                    items(assistants) { profile ->
                        StaffCard(
                            profile = profile,
                            onEditClick = { onEditClick(profile) },
                            onDeleteClick = { onDeleteClick(profile) },
                            isDeletable = profile.id != currentUserId
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                if (nurses.isNotEmpty()) {
                    item { StaffSectionHeader("Nurses", nurses.size) }
                    items(nurses) { profile ->
                        StaffCard(
                            profile = profile,
                            onEditClick = { onEditClick(profile) },
                            onDeleteClick = { onDeleteClick(profile) },
                            isDeletable = profile.id != currentUserId
                        )
                    }
                }
            }
        }
    }

    // Edit Dialog
    if (showEditDialog && editingProfile != null) {
        AlertDialog(
            onDismissRequest = onEditDismiss,
            title = { Text("Edit Staff") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editError.isNotBlank()) {
                        Text(editError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = onEditFullNameChange,
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = editFullName.isBlank(),
                        supportingText = { if (editFullName.isBlank()) Text("Name is required") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role Dropdown
                    var roleExpanded by remember { mutableStateOf(false) }
                    val displayName = roles.find { it.first == editRole }?.second ?: editRole

                    ExposedDropdownMenuBox(
                        expanded = roleExpanded,
                        onExpandedChange = { roleExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Role *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            roles.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        onEditRoleChange(value)
                                        roleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onEditConfirm, enabled = !isEditing && editFullName.isNotBlank() && editRole.isNotBlank()) {
                    if (isEditing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onEditDismiss, enabled = !isEditing) { Text("Cancel") }
            }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && deletingProfile != null) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("Delete Staff") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (deleteError.isNotBlank()) {
                        Text(deleteError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("Are you sure you want to delete ${deletingProfile!!.full_name} (${deletingProfile!!.email})? This action cannot be undone.")
                    Text("Note: This will permanently remove their account.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm, enabled = !isDeleting) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Delete", style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.error))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss, enabled = !isDeleting) { Text("Cancel") }
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffCard(
    profile: Profile,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isDeletable: Boolean = true
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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

            // Action buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Staff",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (isDeletable) {
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Staff",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
