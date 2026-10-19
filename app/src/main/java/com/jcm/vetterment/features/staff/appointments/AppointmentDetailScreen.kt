package com.jcm.vetterment.features.staff.appointments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.supabase.GetAppointmentData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun AppointmentDetailScreen(
    appointmentId: String,
    navController: NavController
) {
    var appointment by remember { mutableStateOf<Appointment?>(null) }
    var pet by remember { mutableStateOf<Pet?>(null) }
    var owner by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isActioning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var actionError by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelNotes by remember { mutableStateOf("") }

    // Initial load
    LaunchedEffect(appointmentId) {
        isLoading = true
        errorMessage = ""
        try {
            val fetched = GetAppointmentData.getAppointmentById(appointmentId)
            if (fetched == null) {
                errorMessage = "Appointment not found."
                return@LaunchedEffect
            }
            appointment = fetched
            pet = GetPetData.getPetById(fetched.pet_id)
            owner = GetProfileData.getProfileById(fetched.owner_id)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load appointment."
        } finally {
            isLoading = false
        }
    }

    // Action handler
    LaunchedEffect(pendingAction) {
        if (pendingAction.isBlank()) return@LaunchedEffect
        isActioning = true
        actionError = ""
        try {
            when (pendingAction) {
                "CONFIRM" -> {
                    val success = GetAppointmentData.confirmAppointment(appointmentId)
                    if (success) appointment = appointment?.copy(status = "CONFIRMED")
                    else actionError = "Failed to confirm appointment."
                }
                "CANCEL" -> {
                    val success = GetAppointmentData.cancelAppointment(
                        appointmentId, cancelNotes
                    )
                    if (success) appointment = appointment?.copy(status = "CANCELLED")
                    else actionError = "Failed to cancel appointment."
                }
                "NO_SHOW" -> {
                    val success = GetAppointmentData.markNoShow(appointmentId)
                    if (success) appointment = appointment?.copy(status = "NO_SHOW")
                    else actionError = "Failed to mark as no show."
                }
                "CHECK_IN" -> {
                    val success = GetAppointmentData.checkInAppointment(appointmentId)
                    if (success) appointment = appointment?.copy(status = "CHECKED_IN")
                    else actionError = "Failed to check in."
                }
                "OPEN_CASE" -> {
                    val linkedCase = GetAppointmentData.getLinkedCase(appointmentId)
                    if (linkedCase?.id != null) {
                        navController.navigate(CaseRoutes.caseDetails(linkedCase.id))
                    } else {
                        actionError = "No case linked to this appointment."
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            actionError = e.message ?: "An unexpected error occurred."
        } finally {
            isActioning = false
            pendingAction = ""
        }
    }

    AppointmentDetailContent(
        appointment = appointment,
        pet = pet,
        owner = owner,
        isLoading = isLoading,
        isActioning = isActioning,
        errorMessage = errorMessage,
        actionError = actionError,
        onBackClick = { navController.popBackStack() },
        onConfirmClick = { pendingAction = "CONFIRM" },
        onCancelClick = { showCancelDialog = true },
        onNoShowClick = { pendingAction = "NO_SHOW" },
        onCheckInClick = { pendingAction = "CHECK_IN" },
        onOpenCaseClick = { pendingAction = "OPEN_CASE" }
    )

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Appointment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add an optional reason for cancellation.")
                    OutlinedTextField(
                        value = cancelNotes,
                        onValueChange = { cancelNotes = it },
                        label = { Text("Reason (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    pendingAction = "CANCEL"
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Back") }
            }
        )
    }
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailContent(
    appointment: Appointment?,
    pet: Pet?,
    owner: Profile?,
    isLoading: Boolean,
    isActioning: Boolean,
    errorMessage: String,
    actionError: String,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    onNoShowClick: () -> Unit,
    onCheckInClick: () -> Unit,
    onOpenCaseClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        appointment?.appointment_type
                            ?.lowercase()
                            ?.replace("_", " ")
                            ?.replaceFirstChar { it.uppercase() }
                            ?: "Appointment"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage.isNotBlank() -> {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    )
                }
                appointment != null -> {
                    AppointmentDetailBody(
                        appointment = appointment,
                        pet = pet,
                        owner = owner,
                        isActioning = isActioning,
                        actionError = actionError,
                        onConfirmClick = onConfirmClick,
                        onCancelClick = onCancelClick,
                        onNoShowClick = onNoShowClick,
                        onCheckInClick = onCheckInClick,
                        onOpenCaseClick = onOpenCaseClick
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentDetailBody(
    appointment: Appointment,
    pet: Pet?,
    owner: Profile?,
    isActioning: Boolean,
    actionError: String,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    onNoShowClick: () -> Unit,
    onCheckInClick: () -> Unit,
    onOpenCaseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Status chip ───────────────────────────────────────────────────
        Row {
            AppointmentStatusChip(status = appointment.status)
        }

        // ── Patient info ──────────────────────────────────────────────────
        if (pet != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (pet.species.uppercase() == "DOG") "🐶" else "🐱",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(pet.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = pet.breed ?: pet.species.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (owner != null) {
                                Text(
                                    text = "Owner: ${owner.full_name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Appointment details ───────────────────────────────────────────
        AppointmentSection(title = "Appointment Details") {
            AppointmentDetailRow("Type",
                appointment.appointment_type
                    .lowercase().replace("_", " ")
                    .replaceFirstChar { it.uppercase() }
            )
            AppointmentDetailRow("Date", appointment.appointment_date)
            AppointmentDetailRow("Time", appointment.appointment_time.take(5))
            appointment.reason?.takeIf { it.isNotBlank() }?.let {
                AppointmentDetailRow("Reason", it)
            }
            appointment.notes?.takeIf { it.isNotBlank() }?.let {
                AppointmentDetailRow("Notes", it)
            }
        }

        // ── Action error ──────────────────────────────────────────────────
        if (actionError.isNotBlank()) {
            Text(
                text = actionError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        HorizontalDivider()

        // ── Actions ───────────────────────────────────────────────────────
        AppointmentSection(title = "Actions") {
            AppointmentActions(
                status = appointment.status,
                isActioning = isActioning,
                onConfirmClick = onConfirmClick,
                onCancelClick = onCancelClick,
                onNoShowClick = onNoShowClick,
                onCheckInClick = onCheckInClick,
                onOpenCaseClick = onOpenCaseClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AppointmentActions(
    status: String,
    isActioning: Boolean,
    onConfirmClick: () -> Unit,
    onCancelClick: () -> Unit,
    onNoShowClick: () -> Unit,
    onCheckInClick: () -> Unit,
    onOpenCaseClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (status) {
            "PENDING" -> {
                // Confirm or cancel
                Button(
                    onClick = onConfirmClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActioning
                ) { ActionButtonContent("Confirm Appointment", isActioning) }

                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActioning
                ) { Text("Cancel Appointment") }
            }
            "CONFIRMED" -> {
                // Check in, no show, or cancel
                Button(
                    onClick = onCheckInClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActioning
                ) { ActionButtonContent("Check In", isActioning) }

                OutlinedButton(
                    onClick = onNoShowClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActioning
                ) { Text("Mark as No Show") }

                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActioning
                ) { Text("Cancel Appointment") }
            }
            "CHECKED_IN" -> {
                // Navigate to the already-linked case
                Button(
                    onClick = onOpenCaseClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActioning
                ) { ActionButtonContent("Open Case", isActioning) }
            }
            "RESCHEDULED" -> {
                // Patient may still arrive on the new date — same options as CONFIRMED
                Button(
                    onClick = onCheckInClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActioning
                ) { ActionButtonContent("Check In", isActioning) }

                OutlinedButton(
                    onClick = onNoShowClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActioning
                ) { Text("Mark as No Show") }

                OutlinedButton(
                    onClick = onCancelClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isActioning
                ) { Text("Cancel Appointment") }
            }
            "COMPLETED", "CANCELLED", "NO_SHOW" -> {
                Text(
                    text = "This appointment is ${
                        status.lowercase().replace("_", " ")
                    }. No further actions available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionButtonContent(label: String, isActioning: Boolean) {
    if (isActioning) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Please wait…")
    } else {
        Text(label)
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun AppointmentSection(
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
private fun AppointmentDetailRow(label: String, value: String) {
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
