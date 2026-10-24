package com.jcm.vetterment.features.pet_owner



import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.CalendarMonth

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp

import androidx.navigation.NavController

import com.jcm.vetterment.features.data_models.Appointment

import com.jcm.vetterment.features.data_models.Pet

import com.jcm.vetterment.navigation.PetOwnerRoutes

import com.jcm.vetterment.supabase.GetAppointmentData

import com.jcm.vetterment.supabase.GetProfileData
import com.jcm.vetterment.ui.theme.PrimaryGreen
import com.jcm.vetterment.ui.theme.White



// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable

fun PetOwnerAppointmentsScreen(navController: NavController) {

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf("") }



    LaunchedEffect(Unit) {

        isLoading = true

        errorMessage = ""

        try {

            val profile = GetProfileData.getCurrentProfile() ?: run {

                errorMessage = "Unable to load profile."

                return@LaunchedEffect

            }

            appointments = GetAppointmentData.getAppointmentsForOwner(profile.id)

        } catch (e: Exception) {

            e.printStackTrace()

            errorMessage = "Unable to load appointments."

        } finally {

            isLoading = false

        }

    }



    PetOwnerAppointmentsContent(

        navController = navController,

        appointments = appointments,

        isLoading = isLoading,

        errorMessage = errorMessage,

        onAppointmentClick = {

            it.id?.let { id -> navController.navigate(PetOwnerRoutes.appointmentDetail(id)) }

        },

        onBookClick = {

            navController.navigate(PetOwnerRoutes.BookAppointment)

        }

    )

}



// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable

fun PetOwnerAppointmentsContent(

    navController: NavController,

    appointments: List<Appointment>,

    isLoading: Boolean,

    errorMessage: String,

    onAppointmentClick: (Appointment) -> Unit,

    onBookClick: () -> Unit

) {

    // Section ordering: action-needed first, historical last

    val upcoming = appointments.filter {

        it.status in listOf("PENDING", "CONFIRMED", "RESCHEDULED", "CHECKED_IN")

    }.sortedWith(compareBy({ it.appointment_date }, { it.appointment_time }))



    val past = appointments.filter {

        it.status in listOf("COMPLETED", "CANCELLED", "NO_SHOW")

    }.sortedByDescending { it.appointment_date }



    PetOwnerScaffold(

        navController = navController,

        floatingActionButton = {

            ExtendedFloatingActionButton(
                onClick = onBookClick,
                icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                text = { Text("Book a Visit") },
                containerColor = PrimaryGreen,
                contentColor = White
            )

        }

    ) { innerPadding ->

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

                return@PetOwnerScaffold

            }



            if (appointments.isEmpty()) {

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        Text(

                            "No appointments yet.",

                            style = MaterialTheme.typography.bodyMedium,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                        Text(
                            "Book a consultation for your pet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(onClick = onBookClick) {
                            Text("Book Consultation")
                        }

                    }

                }

                return@PetOwnerScaffold

            }



            LazyColumn(

                verticalArrangement = Arrangement.spacedBy(8.dp),

                contentPadding = PaddingValues(bottom = 80.dp)

            ) {

                if (upcoming.isNotEmpty()) {

                    item { OwnerApptSectionHeader("Upcoming", upcoming.size) }

                    items(upcoming) { appt ->

                        OwnerAppointmentCard(

                            appointment = appt,

                            onClick = { onAppointmentClick(appt) }

                        )

                    }

                    item { Spacer(Modifier.height(4.dp)) }

                }

                if (past.isNotEmpty()) {

                    item { OwnerApptSectionHeader("Past", past.size) }

                    items(past) { appt ->

                        OwnerAppointmentCard(

                            appointment = appt,

                            onClick = { onAppointmentClick(appt) }

                        )

                    }

                }

            }

        }

    }

}



// ─── Helper composables ───────────────────────────────────────────────────────

@Composable

private fun OwnerApptSectionHeader(title: String, count: Int) {

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

fun OwnerAppointmentCard(appointment: Appointment, onClick: () -> Unit) {

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

                // Doctor fee for Consultation appointments
                if (appointment.appointment_type == "CONSULTATION") {
                    Text(
                        text = "Doctor's fee: ₱400",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

            }

            OwnerAppointmentStatusChip(status = appointment.status)

        }

    }

}



@Composable

fun OwnerAppointmentStatusChip(status: String) {

    val containerColor = when (status) {

        "PENDING"     -> MaterialTheme.colorScheme.secondaryContainer

        "CONFIRMED"   -> MaterialTheme.colorScheme.primaryContainer

        "CHECKED_IN"  -> MaterialTheme.colorScheme.tertiaryContainer

        "COMPLETED"   -> MaterialTheme.colorScheme.tertiaryContainer

        "RESCHEDULED" -> MaterialTheme.colorScheme.secondaryContainer

        "CANCELLED",

        "NO_SHOW"     -> MaterialTheme.colorScheme.errorContainer

        else          -> MaterialTheme.colorScheme.surfaceVariant

    }

    val contentColor = when (status) {

        "PENDING"     -> MaterialTheme.colorScheme.onSecondaryContainer

        "CONFIRMED"   -> MaterialTheme.colorScheme.onPrimaryContainer

        "CHECKED_IN"  -> MaterialTheme.colorScheme.onTertiaryContainer

        "COMPLETED"   -> MaterialTheme.colorScheme.onTertiaryContainer

        "RESCHEDULED" -> MaterialTheme.colorScheme.onSecondaryContainer

        "CANCELLED",

        "NO_SHOW"     -> MaterialTheme.colorScheme.onErrorContainer

        else          -> MaterialTheme.colorScheme.onSurfaceVariant

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

