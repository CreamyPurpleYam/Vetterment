package com.jcm.vetterment.features.pet_owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.Medication
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.navigation.PetOwnerRoutes
import com.jcm.vetterment.supabase.GetAppointmentData
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetMedicationData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun PetOwner_Dashboard(navController: NavController) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var activeCases by remember { mutableStateOf<List<Pair<Case, Pet?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf<List<Pair<Medication, Pet?>>>(emptyList()) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = ""
        try {
            val currentProfile = GetProfileData.getCurrentProfile()
            profile = currentProfile

            if (currentProfile != null) {
                val ownedPets = GetPetData.getPetsForOwner(currentProfile.id)
                pets = ownedPets
                appointments = GetAppointmentData.getAppointmentsForOwner(currentProfile.id)

                val cases = mutableListOf<Pair<Case, Pet?>>()
                ownedPets.forEach { pet ->
                    val petId = pet.id ?: return@forEach
                    val petCases = GetCaseData.getCasesForPet(petId)
                    petCases
                        .filter { it.current_status !in listOf("COMPLETED", "CANCELLED", "DISCHARGED") }
                        .forEach { case -> cases.add(case to pet) }
                }
                activeCases = cases

                val allMedications = mutableListOf<Pair<Medication, Pet?>>()
                activeCases.forEach { (case, pet) ->
                    val caseMeds = GetMedicationData.getMedicationsForCase(case.id ?: return@forEach)
                    caseMeds.filter { it.is_active }.forEach { med ->
                        allMedications.add(med to pet)
                    }
                }
                medications = allMedications
            } else {
                errorMessage = "Unable to load profile."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load dashboard."
        } finally {
            isLoading = false
        }
    }

    val upcomingAppointments = appointments
        .filter { it.status in listOf("PENDING", "CONFIRMED", "CHECKED_IN", "RESCHEDULED") }
        .sortedWith(compareBy({ it.appointment_date }, { it.appointment_time }))

    PetOwnerDashboardContent(
        navController = navController,
        ownerName = profile?.full_name,
        isLoading = isLoading,
        errorMessage = errorMessage,
        pets = pets,
        upcomingAppointments = upcomingAppointments,
        medications = medications
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun PetOwnerDashboardContent(
    navController: NavController,
    ownerName: String?,
    isLoading: Boolean,
    errorMessage: String,
    pets: List<Pet>,
    upcomingAppointments: List<Appointment>,
    medications: List<Pair<Medication, Pet?>>
) {
    PetOwnerScaffold(navController = navController) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (!ownerName.isNullOrBlank()) "Welcome, $ownerName" else "Welcome",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Manage your pets, appointments, and care updates in one place.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // My Pets section (Priority A)
                    DashboardSection(title = "My Pets") {
                        if (pets.isEmpty()) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(PetOwnerRoutes.MyPets) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "No pets registered yet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tap to view pet management",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(pets) { pet ->
                                    DashboardPetCard(
                                        pet = pet,
                                        onClick = {
                                            pet.id?.let { id ->
                                                navController.navigate(PetOwnerRoutes.petDetail(id))
                                            } ?: navController.navigate(PetOwnerRoutes.MyPets)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Upcoming appointments (Priority B)
                    DashboardSection(
                        title = "Upcoming Appointments",
                        action = {
                            IconButton(onClick = { navController.navigate(PetOwnerRoutes.BookAppointment) }) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Book Appointment",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    ) {
                        if (upcomingAppointments.isEmpty()) {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { navController.navigate(PetOwnerRoutes.BookAppointment) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "No current appointments.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Book now?",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = { navController.navigate(PetOwnerRoutes.BookAppointment) },
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Book Appointment"
                                        )
                                    }
                                }
                            }
                        } else {
                            upcomingAppointments.take(3).forEach { appt ->
                                val matchingPet = pets.find { it.id == appt.pet_id }
                                DashboardAppointmentCard(
                                    appointment = appt,
                                    petName = matchingPet?.name ?: "Pet",
                                    onClick = {
                                        appt.id?.let { id ->
                                            navController.navigate(PetOwnerRoutes.appointmentDetail(id))
                                        } ?: navController.navigate(PetOwnerRoutes.Appointments)
                                    }
                                )
                            }
                        }
                    }

                    // Vaccination Reminders (Priority C)
                    DashboardSection(title = "Vaccination Reminders") {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("\uD83D\uDC89", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Vaccination Reminder",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "No upcoming vaccinations due.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "A reminder will appear here when a pet's vaccination is nearing its due date.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Today's Medications (Priority D)
                    DashboardSection(title = "Today's Medications") {
                        if (medications.isEmpty()) {
                            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "No medications for today.",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tomorrow: No medications scheduled.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            medications.take(3).forEach { (med, pet) ->
                                DashboardMedicationCard(medication = med, pet = pet)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (medications.size > 3) {
                                TextButton(onClick = { navController.navigate(PetOwnerRoutes.Medications) }) {
                                    Text("See all ${medications.size} medications")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun DashboardSection(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            action?.invoke()
        }
        content()
    }
}

@Composable
private fun DashboardPetCard(
    pet: Pet,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .width(130.dp)
            .height(140.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (!pet.photo_url.isNullOrBlank()) {
                            pet.photo_url
                        } else {
                            when (pet.species.uppercase()) {
                                "DOG" -> "\uD83D\uDC36"
                                "CAT" -> "\uD83D\uDC31"
                                else  -> "\uD83D\uDC3E"
                            }
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = pet.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!pet.nickname.isNullOrBlank()) {
                Text(
                    text = "\"${pet.nickname}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = pet.breed?.takeIf { it.isNotBlank() }
                    ?: pet.species.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DashboardAppointmentCard(
    appointment: Appointment,
    petName: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$petName \u2022 ${
                        appointment.appointment_type
                            .lowercase()
                            .replace("_", " ")
                            .replaceFirstChar { it.uppercase() }
                    }",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${appointment.appointment_date} at ${appointment.appointment_time.take(5)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                appointment.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            AssistChip(
                onClick = onClick,
                label = {
                    Text(
                        appointment.status
                            .split("_")
                            .joinToString(" ") { word ->
                                word.lowercase().replaceFirstChar { it.uppercase() }
                            }
                    )
                }
            )
        }
    }
}

@Composable
private fun DashboardMedicationCard(
    medication: Medication,
    pet: Pet?
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val timeLabel = medication.scheduled_times?.firstOrNull() ?: "Scheduled today"
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${medication.name} (${medication.dosage})",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "For ${pet?.name ?: "Pet"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("\uD83D\uDC8A", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
