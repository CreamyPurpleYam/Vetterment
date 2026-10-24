package com.jcm.vetterment.features.staff.appointments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.staff.StaffScaffold
import com.jcm.vetterment.navigation.AppointmentRoutes
import com.jcm.vetterment.supabase.GetAppointmentData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun AppointmentsScreen(navController: NavController) {
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = ""
        try {
            appointments = GetAppointmentData.getAllAppointments()
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load appointments."
        } finally {
            isLoading = false
        }
    }

    AppointmentsContent(
        navController = navController,
        appointments = appointments,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onAppointmentClick = {
            it.id?.let { id -> navController.navigate(AppointmentRoutes.appointmentDetail(id)) }
        }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun AppointmentsContent(
    navController: NavController,
    appointments: List<Appointment>,
    isLoading: Boolean,
    errorMessage: String,
    onAppointmentClick: (Appointment) -> Unit
) {
    val pending     = appointments.filter { it.status == "PENDING" }
    val confirmed   = appointments.filter { it.status == "CONFIRMED" }
    val checkedIn   = appointments.filter { it.status == "CHECKED_IN" }
    val rescheduled = appointments.filter { it.status == "RESCHEDULED" }
    val completed   = appointments.filter { it.status == "COMPLETED" }
    val cancelled   = appointments.filter { it.status == "CANCELLED" }
    val noShow      = appointments.filter { it.status == "NO_SHOW" }

    StaffScaffold(navController = navController) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text("Appointments", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@StaffScaffold
            }

            if (appointments.isEmpty()) {
                Text(
                    "No appointments yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@StaffScaffold
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Checked in — needs immediate action
                if (checkedIn.isNotEmpty()) {
                    item { AppointmentSectionHeader("Checked In", checkedIn.size) }
                    items(checkedIn) { appt ->
                        AppointmentCard(appointment = appt, onClick = { onAppointmentClick(appt) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                // Pending — needs staff review
                if (pending.isNotEmpty()) {
                    item { AppointmentSectionHeader("Pending", pending.size) }
                    items(pending) { appt ->
                        AppointmentCard(appointment = appt, onClick = { onAppointmentClick(appt) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                if (confirmed.isNotEmpty()) {
                    item { AppointmentSectionHeader("Confirmed", confirmed.size) }
                    items(confirmed) { appt ->
                        AppointmentCard(appointment = appt, onClick = { onAppointmentClick(appt) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                if (rescheduled.isNotEmpty()) {
                    item { AppointmentSectionHeader("Rescheduled", rescheduled.size) }
                    items(rescheduled) { appt ->
                        AppointmentCard(appointment = appt, onClick = { onAppointmentClick(appt) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                if (completed.isNotEmpty()) {
                    item { AppointmentSectionHeader("Completed", completed.size) }
                    items(completed) { appt ->
                        AppointmentCard(appointment = appt, onClick = { onAppointmentClick(appt) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                if (cancelled.isNotEmpty()) {
                    item { AppointmentSectionHeader("Cancelled", cancelled.size) }
                    items(cancelled) { appt ->
                        AppointmentCard(appointment = appt, onClick = { onAppointmentClick(appt) })
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }
                if (noShow.isNotEmpty()) {
                    item { AppointmentSectionHeader("No Show", noShow.size) }
                    items(noShow) { appt ->
                        AppointmentCard(appointment = appt, onClick = { onAppointmentClick(appt) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
fun AppointmentCard(appointment: Appointment, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.appointment_type
                        .lowercase()
                        .replace("_", " ")
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${appointment.appointment_date} at ${
                        appointment.appointment_time.take(5)
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                appointment.reason?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AppointmentStatusChip(status = appointment.status)
        }
    }
}

@Composable
fun AppointmentStatusChip(status: String) {
    val containerColor = when (status) {
        "PENDING"    -> MaterialTheme.colorScheme.secondaryContainer
        "CONFIRMED"  -> MaterialTheme.colorScheme.primaryContainer
        "CHECKED_IN" -> MaterialTheme.colorScheme.tertiaryContainer
        "COMPLETED"  -> MaterialTheme.colorScheme.tertiaryContainer
        "CANCELLED",
        "NO_SHOW"    -> MaterialTheme.colorScheme.errorContainer
        else         -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (status) {
        "PENDING"    -> MaterialTheme.colorScheme.onSecondaryContainer
        "CONFIRMED"  -> MaterialTheme.colorScheme.onPrimaryContainer
        "CHECKED_IN" -> MaterialTheme.colorScheme.onTertiaryContainer
        "COMPLETED"  -> MaterialTheme.colorScheme.onTertiaryContainer
        "CANCELLED",
        "NO_SHOW"    -> MaterialTheme.colorScheme.onErrorContainer
        else         -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.small, color = containerColor) {
        Text(
            text = status.replace("_", " ")
                .lowercase()
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
