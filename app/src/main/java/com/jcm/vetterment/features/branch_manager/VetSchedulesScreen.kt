package com.jcm.vetterment.features.branchmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.components.DatePickerField
import com.jcm.vetterment.features.components.TimePickerField
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.features.data_models.Roles
import com.jcm.vetterment.features.data_models.VetSchedule
import com.jcm.vetterment.features.data_models.VetShifts
import com.jcm.vetterment.supabase.GetProfileData
import com.jcm.vetterment.supabase.GetScheduleData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun VetSchedulesScreen(navController: NavController) {
    var selectedDate by remember { mutableStateOf(todayDateString()) }
    var schedules by remember { mutableStateOf<List<VetSchedule>>(emptyList()) }
    var vets by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var currentRole by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // Add/edit dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<VetSchedule?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }

    // Delete dialog state
    var scheduleToDelete by remember { mutableStateOf<VetSchedule?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            isLoading = true
            errorMessage = ""
            try {
                schedules = GetScheduleData.getSchedulesForDate(selectedDate)
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Unable to load schedules."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        try {
            currentRole = GetProfileData.getCurrentRole()
            // Only veterinarian-level staff can be scheduled for duty.
            vets = GetProfileData.getAllStaff().filter { Roles.isVetLevel(it.role) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Reload the roster whenever the picked day changes (also handles first load).
    LaunchedEffect(selectedDate) {
        refresh()
    }

    fun saveSchedule(schedule: VetSchedule) {
        scope.launch {
            isSaving = true
            saveError = ""
            val result = GetScheduleData.saveSchedule(schedule)
            if (result.isSuccess) {
                showAddDialog = false
                editingSchedule = null
                refresh()
            } else {
                saveError = result.exceptionOrNull()?.message ?: "Failed to save schedule."
            }
            isSaving = false
        }
    }

    fun deleteSchedule(schedule: VetSchedule) {
        scope.launch {
            isDeleting = true
            deleteError = ""
            try {
                if (GetScheduleData.deleteSchedule(schedule.id ?: "")) {
                    scheduleToDelete = null
                    refresh()
                } else {
                    deleteError = "Failed to remove the entry. It may already be gone — try refreshing."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                deleteError = "Something went wrong."
            } finally {
                isDeleting = false
            }
        }
    }

    VetSchedulesContent(
        selectedDate = selectedDate,
        schedules = schedules,
        vets = vets,
        isLoading = isLoading,
        errorMessage = errorMessage,
        isManager = Roles.isStaffManager(currentRole),
        showAddDialog = showAddDialog,
        editingSchedule = editingSchedule,
        isSaving = isSaving,
        saveError = saveError,
        scheduleToDelete = scheduleToDelete,
        isDeleting = isDeleting,
        deleteError = deleteError,
        onBackClick = { navController.popBackStack() },
        onDateChange = { selectedDate = it },
        onAddClick = {
            editingSchedule = null
            saveError = ""
            showAddDialog = true
        },
        onEditClick = { schedule ->
            editingSchedule = schedule
            saveError = ""
            showAddDialog = true
        },
        onDeleteClick = { schedule ->
            scheduleToDelete = schedule
            deleteError = ""
        },
        onDismissAddDialog = {
            showAddDialog = false
            editingSchedule = null
            saveError = ""
        },
        onSave = { saveSchedule(it) },
        onDeleteConfirm = { scheduleToDelete?.let { deleteSchedule(it) } },
        onDeleteDismiss = {
            scheduleToDelete = null
            deleteError = ""
        }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetSchedulesContent(
    selectedDate: String,
    schedules: List<VetSchedule>,
    vets: List<Profile>,
    isLoading: Boolean,
    errorMessage: String,
    isManager: Boolean,
    showAddDialog: Boolean,
    editingSchedule: VetSchedule?,
    isSaving: Boolean,
    saveError: String,
    scheduleToDelete: VetSchedule?,
    isDeleting: Boolean,
    deleteError: String,
    onBackClick: () -> Unit,
    onDateChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (VetSchedule) -> Unit,
    onDeleteClick: (VetSchedule) -> Unit,
    onDismissAddDialog: () -> Unit,
    onSave: (VetSchedule) -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit
) {
    val vetsById = vets.associateBy { it.id }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vet Schedules") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isManager) {
                FloatingActionButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add schedule")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Assign which veterinarian covers each shift per day. Pet owners see this roster when booking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DatePickerField(
                value = selectedDate,
                label = "Schedule Date",
                onValueChange = onDateChange
            )

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }

            if (!isManager) {
                Text(
                    text = "Only the head veterinarian can manage schedules.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                schedules.isEmpty() -> {
                    Text(
                        text = "No schedules posted for this day yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        VetShifts.ALL.forEach { shift ->
                            val shiftSchedules = schedules.filter { it.shift == shift }
                            if (shiftSchedules.isNotEmpty()) {
                                item {
                                    ShiftHeader(shift = shift, count = shiftSchedules.size)
                                }
                                items(shiftSchedules) { schedule ->
                                    ScheduleCard(
                                        schedule = schedule,
                                        vet = vetsById[schedule.vet_id],
                                        isManager = isManager,
                                        onEditClick = { onEditClick(schedule) },
                                        onDeleteClick = { onDeleteClick(schedule) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        AddScheduleDialog(
            vets = vets,
            editing = editingSchedule,
            defaultDate = selectedDate,
            isSaving = isSaving,
            saveError = saveError,
            onDismiss = onDismissAddDialog,
            onSave = onSave
        )
    }

    // Delete Dialog
    if (scheduleToDelete != null) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("Remove Schedule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (deleteError.isNotBlank()) {
                        Text(
                            deleteError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val vetName = vetsById[scheduleToDelete?.vet_id]?.full_name ?: "This veterinarian"
                    Text(
                        "Remove $vetName from the " +
                                "${VetShifts.label(scheduleToDelete?.shift ?: "")} duty list " +
                                "for ${scheduleToDelete?.schedule_date}? " +
                                "They will no longer appear as on duty that day."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDeleteConfirm, enabled = !isDeleting) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            "Remove",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss, enabled = !isDeleting) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── Functions ────────────────────────────────────────────────────────────────

@Composable
private fun ShiftHeader(shift: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = VetShifts.label(shift),
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
private fun ScheduleCard(
    schedule: VetSchedule,
    vet: Profile?,
    isManager: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vet?.full_name ?: "Unknown staff",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = Roles.displayName(vet?.role),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${schedule.start_time} – ${schedule.end_time}",
                    style = MaterialTheme.typography.bodyMedium
                )
                schedule.notes?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isManager) {
                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit schedule",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete schedule",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddScheduleDialog(
    vets: List<Profile>,
    editing: VetSchedule?,
    defaultDate: String,
    isSaving: Boolean,
    saveError: String,
    onDismiss: () -> Unit,
    onSave: (VetSchedule) -> Unit
) {
    var selectedVetId by remember { mutableStateOf(editing?.vet_id ?: "") }
    var date by remember { mutableStateOf(editing?.schedule_date ?: defaultDate) }
    var shift by remember { mutableStateOf(editing?.shift ?: "") }
    var startTime by remember { mutableStateOf(editing?.start_time ?: "") }
    var endTime by remember { mutableStateOf(editing?.end_time ?: "") }
    var notes by remember { mutableStateOf(editing?.notes ?: "") }
    var validationError by remember { mutableStateOf("") }
    var vetMenuExpanded by remember { mutableStateOf(false) }

    val selectedVet = vets.find { it.id == selectedVetId }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(if (editing == null) "Add Schedule" else "Edit Schedule") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError.isNotBlank()) {
                    Text(
                        validationError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (saveError.isNotBlank()) {
                    Text(
                        saveError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Veterinarian dropdown
                ExposedDropdownMenuBox(
                    expanded = vetMenuExpanded,
                    onExpandedChange = { vetMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedVet?.full_name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Veterinarian *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = vetMenuExpanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = vetMenuExpanded,
                        onDismissRequest = { vetMenuExpanded = false }
                    ) {
                        if (vets.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No veterinarians found") },
                                onClick = { }
                            )
                        }
                        vets.forEach { vet ->
                            DropdownMenuItem(
                                text = { Text("${vet.full_name} (${Roles.displayName(vet.role)})") },
                                onClick = {
                                    selectedVetId = vet.id
                                    validationError = ""
                                    vetMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                DatePickerField(
                    value = date,
                    label = "Date *",
                    onValueChange = { date = it; validationError = "" }
                )

                // Shift
                Text("Shift *", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VetShifts.ALL.forEach { s ->
                        FilterChip(
                            selected = shift == s,
                            onClick = { shift = s; validationError = "" },
                            label = { Text(VetShifts.shortLabel(s)) }
                        )
                    }
                }

                TimePickerField(
                    value = startTime,
                    label = "Start Time (24-hour)",
                    onValueChange = { startTime = it; validationError = "" }
                )

                TimePickerField(
                    value = endTime,
                    label = "End Time (24-hour)",
                    onValueChange = { endTime = it; validationError = "" }
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        selectedVetId.isBlank() -> validationError = "Please choose a veterinarian."
                        date.isBlank() -> validationError = "Please pick a date."
                        shift.isBlank() -> validationError = "Please choose a shift."
                        startTime.isBlank() -> validationError = "Please pick a start time."
                        endTime.isBlank() -> validationError = "Please pick an end time."
                        else -> onSave(
                            VetSchedule(
                                id = editing?.id,
                                vet_id = selectedVetId,
                                schedule_date = date,
                                shift = shift,
                                start_time = startTime,
                                end_time = endTime,
                                notes = notes.trim().takeIf { it.isNotBlank() },
                                created_by = editing?.created_by
                            )
                        )
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (editing == null) "Add" else "Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

private fun todayDateString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
