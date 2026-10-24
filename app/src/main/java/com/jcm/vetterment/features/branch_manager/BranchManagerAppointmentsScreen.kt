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
import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.supabase.GetAppointmentData
import com.jcm.vetterment.supabase.GetProfileData
import kotlinx.coroutines.launch

@Composable
fun BranchManagerAppointmentsScreen(navController: NavController) {
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

    BranchManagerAppointmentsContent(
        navController = navController,
        appointments = appointments,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onAppointmentClick = { it.id?.let { id -> navController.navigate(CaseRoutes.caseDetails(id)) } },
        onRefresh = { /* refresh handled by LaunchedEffect */ }
    )
}

@Composable
fun BranchManagerAppointmentsContent(
    navController: NavController,
    appointments: List<Appointment>,
    isLoading: Boolean,
    errorMessage: String,
    onAppointmentClick: (Appointment) -> Unit,
    onRefresh: () -> Unit
) {
    BranchManagerScaffold(navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                return@BranchManagerScaffold
            }

            if (appointments.isEmpty()) {
                Text(
                    text = "No appointments found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@BranchManagerScaffold
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
            ) {
                items(appointments) { appointment ->
                    AppointmentCard(
                        appointment = appointment,
                        onClick = { onAppointmentClick(appointment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.appointment_type.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${appointment.appointment_date} • ${appointment.reason ?: "No reason provided"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Status chip
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when (appointment.status.lowercase()) {
                    "pending"     -> MaterialTheme.colorScheme.primaryContainer
                    "confirmed"   -> MaterialTheme.colorScheme.tertiaryContainer
                    "checked_in"  -> MaterialTheme.colorScheme.secondaryContainer
                    "completed"   -> MaterialTheme.colorScheme.tertiaryContainer
                    "cancelled"   -> MaterialTheme.colorScheme.errorContainer
                    else          -> MaterialTheme.colorScheme.surfaceContainer
                }
            ) {
                Text(
                    text = appointment.status.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (appointment.status.lowercase()) {
                        "pending"     -> MaterialTheme.colorScheme.onPrimaryContainer
                        "confirmed"   -> MaterialTheme.colorScheme.onTertiaryContainer
                        "checked_in"  -> MaterialTheme.colorScheme.onSecondaryContainer
                        "completed"   -> MaterialTheme.colorScheme.onTertiaryContainer
                        "cancelled"   -> MaterialTheme.colorScheme.onErrorContainer
                        else          -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}