package com.jcm.vetterment.features.pet_owner

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
import com.jcm.vetterment.supabase.GetAppointmentData
import com.jcm.vetterment.supabase.GetPetData
import kotlinx.coroutines.launch

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun PetOwnerAppointmentDetailScreen(
    appointmentId: String,
    navController: NavController
) {
    var appointment by remember { mutableStateOf<Appointment?>(null) }
    var pet by remember { mutableStateOf<Pet?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var showCancelDialog by remember { mutableStateOf(false) }
    var isCancelling by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(appointmentId) {
        isLoading = true
        errorMessage = ""
        try {
            val appt = GetAppointmentData.getAppointmentById(appointmentId) ?: run {
                errorMessage = "Appointment not found."
                return@LaunchedEffect
            }
            appointment = appt
            pet = GetPetData.getPetById(appt.pet_id)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load appointment."
        } finally {
            isLoading = false
        }
    }

    PetOwnerAppointmentDetailContent(
        appointment = appointment,
        pet = pet,
        isLoading = isLoading,
        errorMessage = errorMessage,
        isCancelling = isCancelling,
        onBackClick = { navController.popBackStack() },
        onCancelClick = { showCancelDialog = true }
    )

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Appointment") },
            text = {
                Text("Are you sure you want to cancel this appointment? The clinic will be notified.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        isCancelling = true
                        coroutineScope.launch {
                            try {
                                val success = GetAppointmentData.cancelAppointment(appointmentId)
                                if (success) {
                                    appointment = appointment?.copy(status = "CANCELLED")
                                } else {
                                    errorMessage = "Failed to cancel appointment."
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                errorMessage = "Something went wrong."
                            } finally {
                                isCancelling = false
                            }
                        }
                    }
                ) { Text("Yes, Cancel") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("Keep") }
            }
        )
    }
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetOwnerAppointmentDetailContent(
    appointment: Appointment?,
    pet: Pet?,
    isLoading: Boolean,
    errorMessage: String,
    isCancelling: Boolean,
    onBackClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appointment Details") },
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
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                errorMessage.isNotBlank() && appointment == null -> Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
                appointment != null -> PetOwnerAppointmentDetailBody(
                    appointment = appointment,
                    pet = pet,
                    isCancelling = isCancelling,
                    errorMessage = errorMessage,
                    onCancelClick = onCancelClick
                )
            }
        }
    }
}

// ─── Body ─────────────────────────────────────────────────────────────────────

@Composable
private fun PetOwnerAppointmentDetailBody(
    appointment: Appointment,
    pet: Pet?,
    isCancelling: Boolean,
    errorMessage: String,
    onCancelClick: () -> Unit
) {
    val isCancellable = appointment.status in listOf("PENDING", "CONFIRMED", "RESCHEDULED")
    val isTerminal = appointment.status in listOf("COMPLETED", "CANCELLED", "NO_SHOW")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Status chip ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = appointment.appointment_type
                    .lowercase()
                    .replace("_", " ")
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleLarge
            )
            OwnerAppointmentStatusChip(status = appointment.status)
        }

        // ── Pet info ──────────────────────────────────────────────────────
        pet?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (it.species.uppercase()) {
                            "DOG" -> "🐶"
                            "CAT" -> "🐱"
                            else  -> "🐾"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(it.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = it.breed ?: it.species.lowercase()
                                .replaceFirstChar { c -> c.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Schedule details ──────────────────────────────────────────────
        HorizontalDivider()
        Text(
            "Schedule",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        OwnerDetailRow("Date", appointment.appointment_date)
        OwnerDetailRow("Time", appointment.appointment_time.take(5))
        appointment.reason?.takeIf { it.isNotBlank() }?.let {
            OwnerDetailRow("Reason", it)
        }
        appointment.notes?.takeIf { it.isNotBlank() }?.let {
            OwnerDetailRow("Clinic Notes", it)
        }

        // ── Status message ────────────────────────────────────────────────
        if (!isTerminal) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (appointment.status) {
                        "PENDING"     -> "Your request is awaiting clinic review."
                        "CONFIRMED"   -> "Your appointment is confirmed."
                        "RESCHEDULED" -> "Your appointment has been rescheduled. Please review the updated date and time."
                        "CHECKED_IN"  -> "You have checked in. Please wait to be called."
                        else          -> ""
                    },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (errorMessage.isNotBlank()) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }

        // ── Cancel button ─────────────────────────────────────────────────
        if (isCancellable) {
            OutlinedButton(
                onClick = onCancelClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCancelling,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancelling…")
                } else {
                    Text("Cancel Appointment")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun OwnerDetailRow(label: String, value: String) {
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
