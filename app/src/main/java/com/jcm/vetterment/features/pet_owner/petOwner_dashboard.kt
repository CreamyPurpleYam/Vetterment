package com.jcm.vetterment.features.pet_owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.Medication
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.features.staff.cases.CaseStages
import com.jcm.vetterment.navigation.PetOwnerRoutes
import com.jcm.vetterment.supabase.GetAppointmentData
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetMedicationData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData
import androidx.compose.foundation.layout.Row

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
                        .filter {
                            it.current_status !in listOf("COMPLETED", "CANCELLED", "DISCHARGED")
                        }
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
        activeCases = activeCases,
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
    activeCases: List<Pair<Case, Pet?>>,
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

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = if (!ownerName.isNullOrBlank())
                                    "Welcome, $ownerName"
                                else
                                    "Welcome",
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

                    // Upcoming appointments
                    DashboardSection(title = "Upcoming Appointments") {
                        if (upcomingAppointments.isEmpty()) {
                            Text(
                                text = "No upcoming appointments.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            upcomingAppointments.take(3).forEach { appt ->
                                OwnerAppointmentCard(
                                    appointment = appt,
                                    onClick = {
                                        navController.navigate(PetOwnerRoutes.Appointments)
                                    }
                                )
                            }
                        }
                    }

                    // Active cases
                    DashboardSection(title = "Active Cases") {
                        if (activeCases.isEmpty()) {
                            Text(
                                text = "No active cases right now.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            activeCases.take(3).forEach { (case, pet) ->
                                ActiveCaseCard(
                                    case = case,
                                    pet = pet,
                                    onClick = {
                                        navController.navigate(PetOwnerRoutes.Cases)
                                    }
                                )
                            }
                        }
                    }

                    DashboardSection(title = "Medications") {
                        if (medications.isEmpty()) {
                            Text(
                                text = "No active medications.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            medications.take(3).forEach { (med, pet) ->
                                OwnerMedicationCard(medication = med, pet = pet)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (medications.size > 3) {
                                TextButton(onClick = { navController.navigate(PetOwnerRoutes.Medications) }) {
                                    Text("See all ${medications.size} medications")
                                }
                            }
                        }
                    }

                    // Quick actions
                    DashboardSection(title = "Quick Actions") {
                        QuickActionCard(
                            title = "My Pets",
                            subtitle = "View pets and request changes",
                            onClick = { navController.navigate(PetOwnerRoutes.MyPets) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        QuickActionCard(
                            title = "Appointments",
                            subtitle = "Request, view, or reschedule visits",
                            onClick = { navController.navigate(PetOwnerRoutes.Appointments) }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        QuickActionCard(
                            title = "Cases",
                            subtitle = "Track your pets' active care",
                            onClick = { navController.navigate(PetOwnerRoutes.Cases) }
                        )
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    ElevatedCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveCaseCard(
    case: Case,
    pet: Pet?,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
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
                    text = pet?.name ?: "Unknown Pet",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = case.case_type
                        .lowercase()
                        .replace("_", " ")
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(CaseStages.displayName(case.current_status)) }
            )
        }
    }
}
