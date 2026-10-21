package com.jcm.vetterment.features.pet_owner



import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.LazyRow

import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material.icons.automirrored.filled.NavigateNext

import androidx.compose.material.icons.filled.Add

import androidx.compose.material.icons.filled.CalendarMonth

import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.LocalPharmacy

import androidx.compose.material.icons.filled.MedicalServices

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.navigation.NavController

import com.jcm.vetterment.features.data_models.Appointment

import com.jcm.vetterment.features.data_models.Case

import com.jcm.vetterment.features.data_models.Medication

import com.jcm.vetterment.features.data_models.Pet

import com.jcm.vetterment.features.staff.cases.CaseStages

import com.jcm.vetterment.navigation.PetOwnerRoutes

import com.jcm.vetterment.supabase.GetAppointmentData

import com.jcm.vetterment.supabase.GetCaseData

import com.jcm.vetterment.supabase.GetMedicationData

import com.jcm.vetterment.supabase.GetPetData

import com.jcm.vetterment.supabase.GetProfileData

import kotlinx.coroutines.launch
import com.jcm.vetterment.ui.theme.PrimaryGreen
import com.jcm.vetterment.ui.theme.White



// ─── Logic layer ──────────────────────────────────────────────────────────────



@Composable

fun MyPetsScreen(navController: NavController) {

    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }

    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }

    var medicationsByPetId by remember { mutableStateOf<Map<String, List<Medication>>>(emptyMap()) }

    var ownerId by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf("") }

    var showAddPetDialog by remember { mutableStateOf(false) }

    var isCreatingPet by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()



    val currentEntry = navController.currentBackStackEntry

    LaunchedEffect(currentEntry) {

        isLoading = true

        errorMessage = ""

        try {

            val profile = GetProfileData.getCurrentProfile() ?: run {

                errorMessage = "Unable to load profile."

                return@LaunchedEffect

            }

            ownerId = profile.id

            val ownedPets = GetPetData.getPetsForOwner(profile.id)

            pets = ownedPets



            val appts = GetAppointmentData.getAppointmentsForOwner(profile.id)

            appointments = appts



            val medsMap = mutableMapOf<String, List<Medication>>()

            ownedPets.forEach { pet ->

                val pId = pet.id ?: return@forEach

                val cases = GetCaseData.getCasesForPet(pId)

                val activeCases = cases.filter { it.current_status !in listOf("COMPLETED", "CANCELLED", "DISCHARGED") }

                val petMeds = mutableListOf<Medication>()

                activeCases.forEach { c ->

                    c.id?.let { caseId ->

                        petMeds.addAll(GetMedicationData.getMedicationsForCase(caseId).filter { it.is_active })

                    }

                }

                if (petMeds.isNotEmpty()) {

                    medsMap[pId] = petMeds

                }

            }

            medicationsByPetId = medsMap

        } catch (e: Exception) {

            e.printStackTrace()

            errorMessage = "Unable to load pets."

        } finally {

            isLoading = false

        }

    }



    val upcomingPetIds = remember(appointments) {

        appointments

            .filter { it.status in listOf("PENDING", "CONFIRMED", "CHECKED_IN", "RESCHEDULED") }

            .map { it.pet_id }

            .toSet()

    }



    if (showAddPetDialog && ownerId.isNotBlank()) {

        AddPetDialog(

            ownerId = ownerId,

            isSaving = isCreatingPet,

            onDismiss = { showAddPetDialog = false },

            onSave = { newPet ->

                scope.launch {

                    isCreatingPet = true

                    val created = GetPetData.createPet(newPet)

                    if (created != null) {

                        showAddPetDialog = false

                        pets = GetPetData.getPetsForOwner(ownerId)

                        created.id?.let { id ->

                            navController.navigate(PetOwnerRoutes.petDetail(id))

                        }

                    } else {

                        errorMessage = "Unable to add pet. Please check fields and try again."

                    }

                    isCreatingPet = false

                }

            }

        )

    }



    MyPetsContent(

        navController = navController,

        pets = pets,

        upcomingPetIds = upcomingPetIds,

        medicationPetIds = medicationsByPetId.keys,

        isLoading = isLoading,

        errorMessage = errorMessage,

        onPetClick = { navController.navigate(PetOwnerRoutes.petDetail(it.id!!)) },

        onAddPetClick = { showAddPetDialog = true }

    )

}



// ─── UI layer ─────────────────────────────────────────────────────────────────



@Composable

fun MyPetsContent(

    navController: NavController,

    pets: List<Pet>,

    upcomingPetIds: Set<String>,

    medicationPetIds: Set<String>,

    isLoading: Boolean,

    errorMessage: String,

    onPetClick: (Pet) -> Unit,

    onAddPetClick: () -> Unit

) {

    PetOwnerScaffold(

        navController = navController,

        floatingActionButton = {

            ExtendedFloatingActionButton(
                onClick = onAddPetClick,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Pet") },
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

            Text("My Pets", style = MaterialTheme.typography.headlineMedium)

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



            if (pets.isEmpty()) {

                Box(

                    modifier = Modifier.fillMaxSize(),

                    contentAlignment = Alignment.Center

                ) {

                    Column(

                        horizontalAlignment = Alignment.CenterHorizontally,

                        verticalArrangement = Arrangement.spacedBy(12.dp)

                    ) {

                        Text(

                            text = "No pets listed yet, add now?",

                            style = MaterialTheme.typography.bodyLarge,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                        Button(onClick = onAddPetClick) {

                            Icon(Icons.Default.Add, contentDescription = null)

                            Spacer(Modifier.width(8.dp))

                            Text("Add Pet")

                        }

                    }

                }

                return@PetOwnerScaffold

            }



            LazyColumn(

                verticalArrangement = Arrangement.spacedBy(10.dp),

                contentPadding = PaddingValues(bottom = 80.dp)

            ) {

                items(pets) { pet ->

                    PetCard(

                        pet = pet,

                        hasUpcomingAppointments = pet.id in upcomingPetIds,

                        hasActiveMeds = pet.id in medicationPetIds,

                        onClick = { onPetClick(pet) }

                    )

                }

            }

        }

    }

}



// ─── Pet detail screen ────────────────────────────────────────────────────────



@Composable

fun PetOwnerPetDetailScreen(petId: String, navController: NavController) {

    var pet by remember { mutableStateOf<Pet?>(null) }

    var medications by remember { mutableStateOf<List<Medication>>(emptyList()) }

    var pastAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }

    var cases by remember { mutableStateOf<List<Case>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }

    var errorMessage by remember { mutableStateOf("") }

    var showEditDialog by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()



    LaunchedEffect(petId) {

        isLoading = true

        errorMessage = ""

        try {

            val fetchedPet = GetPetData.getPetById(petId) ?: run {

                errorMessage = "Pet not found."

                return@LaunchedEffect

            }

            pet = fetchedPet



            val petCases = GetCaseData.getCasesForPet(petId)
            cases = petCases

            val meds = mutableListOf<Medication>()

            cases.forEach { c ->

                c.id?.let { caseId ->

                    meds.addAll(GetMedicationData.getMedicationsForCase(caseId))

                }

            }

            medications = meds



            val profile = GetProfileData.getCurrentProfile()

            if (profile != null) {

                val appts = GetAppointmentData.getAppointmentsForOwner(profile.id)

                pastAppointments = appts.filter { it.pet_id == petId }

            }

        } catch (e: Exception) {

            e.printStackTrace()

            errorMessage = "Unable to load pet."

        } finally {

            isLoading = false

        }

    }



    if (showEditDialog && pet != null) {

        EditPetDialog(

            pet = pet!!,

            isSaving = isSaving,

            onDismiss = { showEditDialog = false },

            onSave = { updatedPet ->

                scope.launch {

                    isSaving = true

                    val result = GetPetData.updatePet(updatedPet)

                    if (result.isSuccess) {

                        pet = updatedPet

                        showEditDialog = false

                    } else {

                        val error = result.exceptionOrNull()
                        errorMessage = "Update failed: ${error?.message ?: "Unknown error"}"

                    }

                    isSaving = false

                }

            }

        )

    }



    PetOwnerPetDetailContent(

        pet = pet,

        medications = medications,

        appointments = pastAppointments,

        cases = cases,

        isLoading = isLoading,

        errorMessage = errorMessage,

        onBackClick = { navController.popBackStack() },

        onEditClick = { showEditDialog = true },

        onViewAllRecordsClick = {
                navController.navigate(PetOwnerRoutes.Cases) {
                    popUpTo(PetOwnerRoutes.Dashboard) { inclusive = false; saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }

    )

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun PetOwnerPetDetailContent(

    pet: Pet?,

    medications: List<Medication>,

    appointments: List<Appointment>,

    cases: List<Case>,

    isLoading: Boolean,

    errorMessage: String,

    onBackClick: () -> Unit,

    onEditClick: () -> Unit = {},

    onViewAllRecordsClick: () -> Unit = {}

) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = { Text(pet?.name ?: "Pet Details") },

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

                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                errorMessage.isNotBlank() -> Text(

                    text = errorMessage,

                    color = MaterialTheme.colorScheme.error,

                    modifier = Modifier.align(Alignment.Center).padding(24.dp)

                )

                pet != null -> PetOwnerPetDetailBody(

                    pet = pet,

                    medications = medications,

                    appointments = appointments,

                    cases = cases,

                    onEditClick = onEditClick,
                    onViewAllRecordsClick = onViewAllRecordsClick

                )

            }

        }

    }

}



@Composable

private fun PetOwnerPetDetailBody(

    pet: Pet,

    medications: List<Medication>,

    appointments: List<Appointment>,

    cases: List<Case>,

    onEditClick: () -> Unit,

    onViewAllRecordsClick: () -> Unit

) {

    val nextVaccination = findNextVaccination(cases, appointments)

    val recentActivity = findRecentHealthActivity(cases, appointments)

    val todaysMedications = getTodaysMedications(medications)

    Column(

        modifier = Modifier

            .fillMaxSize()

            .verticalScroll(rememberScrollState())

            .padding(16.dp),

        verticalArrangement = Arrangement.spacedBy(16.dp)

    ) {

        // Section A: Pet Details
        PetDetailsCard(pet = pet, onEditClick = onEditClick)

        // Section B: Next Vaccination
        NextVaccinationCard(nextVaccination = nextVaccination)

        // Section C: Recent Health Activity
        RecentHealthActivityCard(
            recentActivity = recentActivity,
            onViewAllClick = onViewAllRecordsClick
        )

        // Section D: Medication Schedule (Today)
        MedicationScheduleCard(todaysMedications = todaysMedications)

        Spacer(modifier = Modifier.height(24.dp))

    }

}



// ─── Helper functions for data extraction ────────────────────────────────────

private fun findNextVaccination(cases: List<Case>, appointments: List<Appointment>): Pair<String, String>? {
    val vaccinationCases = cases
        .filter { it.case_type == "VACCINATION" && it.current_status !in listOf("COMPLETED", "CANCELLED", "DISCHARGED") }
        .sortedBy { it.created_at ?: "" }

    if (vaccinationCases.isNotEmpty()) {
        val nextCase = vaccinationCases.first()
        val caseTitle = nextCase.case_type.lowercase().replaceFirstChar { it.uppercase() }
        val linkedAppt = appointments.find { it.linked_case_id == nextCase.id }
        val dueDate = linkedAppt?.appointment_date ?: nextCase.created_at?.take(10) ?: "Date not specified"
        return Pair(caseTitle, dueDate)
    }

    val upcomingVaccAppointments = appointments
        .filter { it.appointment_type == "VACCINATION" && it.status in listOf("PENDING", "CONFIRMED", "CHECKED_IN", "RESCHEDULED") }
        .sortedBy { it.appointment_date }

    if (upcomingVaccAppointments.isNotEmpty()) {
        val appt = upcomingVaccAppointments.first()
        val apptTitle = appt.appointment_type.lowercase().replaceFirstChar { it.uppercase() }
        return Pair(apptTitle, appt.appointment_date)
    }

    return null
}

private fun findRecentHealthActivity(cases: List<Case>, appointments: List<Appointment>): List<HealthActivityItem> {
    val activities = mutableListOf<HealthActivityItem>()

    val recentCases = cases
        .filter { it.current_status !in listOf("CANCELLED") }
        .sortedByDescending { it.created_at ?: "" }
        .take(3)

    recentCases.forEach { c ->
        activities.add(HealthActivityItem(
            title = c.case_type.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
            date = c.created_at?.take(10) ?: "Date not available",
            summary = CaseStages.displayName(c.current_status),
            type = "case",
            id = c.id
        ))
    }

    val recentAppointments = appointments
        .filter { it.status in listOf("COMPLETED", "CHECKED_IN") }
        .sortedByDescending { it.appointment_date }
        .take(3)

    recentAppointments.forEach { appt ->
        activities.add(HealthActivityItem(
            title = appt.appointment_type.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
            date = appt.appointment_date,
            summary = appt.reason?.takeIf { it.isNotBlank() } ?: "Visit completed",
            type = "appointment",
            id = appt.id
        ))
    }

    return activities
        .sortedByDescending { it.date }
        .take(3)
}

private fun getTodaysMedications(medications: List<Medication>): List<Medication> {
    val today = java.time.LocalDate.now().toString()
    return medications.filter { med ->
        med.is_active &&
        med.start_date <= today &&
        (med.end_date == null || med.end_date!! >= today)
    }
}

private data class HealthActivityItem(
    val title: String,
    val date: String,
    val summary: String,
    val type: String,
    val id: String?
)


// ─── Section Composables ─────────────────────────────────────────────────────

@Composable
private fun DetailRow(label: String, value: String, isReadOnly: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = if (isReadOnly) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(0.6f).fillMaxWidth(),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun PetDetailsCard(pet: Pet, onEditClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (!pet.photo_url.isNullOrBlank()) {
                                pet.photo_url
                            } else {
                                when (pet.species.uppercase()) {
                                    "DOG" -> "🐶"
                                    "CAT" -> "🐱"
                                    else -> "🐾"
                                }
                            },
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(pet.name, style = MaterialTheme.typography.headlineSmall)
                    if (!pet.nickname.isNullOrBlank()) {
                        Text(
                            text = "\"${pet.nickname}\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(label = "Species", value = pet.species.lowercase().replaceFirstChar { it.uppercase() })
                pet.breed?.let { DetailRow(label = "Breed", value = it) }
                DetailRow(label = "Sex", value = pet.sex.lowercase().replaceFirstChar { it.uppercase() })
                pet.birth_date?.let { DetailRow(label = "Date of Birth", value = it) }
                pet.weight?.let { DetailRow(label = "Weight", value = "${it} kg") }
                DetailRow(
                    label = "Known Allergies",
                    value = pet.known_allergies?.takeIf { it.isNotBlank() } ?: "None recorded",
                    isReadOnly = true
                )
                DetailRow(
                    label = "Pre-existing Conditions",
                    value = pet.pre_existing_conditions?.takeIf { it.isNotBlank() } ?: "None recorded",
                    isReadOnly = true
                )
            }
        }
    }
}

@Composable
private fun NextVaccinationCard(nextVaccination: Pair<String, String>?) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("💉", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Next Vaccination", style = MaterialTheme.typography.titleMedium)
                    Text("Verified clinic record", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            nextVaccination?.let { (name, dueDate) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(name, style = MaterialTheme.typography.titleSmall)
                            Text("Due: $dueDate", style = MaterialTheme.typography.bodyMedium)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Upcoming", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text(
                        "Vaccination records are managed by clinic staff. Contact your veterinarian for scheduling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } ?: Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No upcoming vaccinations",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Your pet has no verified upcoming vaccinations at this time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HealthActivityItemCard(item: HealthActivityItem) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    Text(item.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = when (item.type) {
                        "case" -> "Medical Record"
                        "appointment" -> "Visit"
                        else -> "Record"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(item.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecentHealthActivityCard(
    recentActivity: List<HealthActivityItem>,
    onViewAllClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📋", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Recent Health Activity", style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = onViewAllClick) {
                    Text("View all")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            if (recentActivity.isEmpty()) {
                Text(
                    "No recent health activity recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentActivity.forEach { item ->
                        HealthActivityItemCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun MedicationItemCard(medication: Medication) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(medication.name, style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${medication.dosage} • ${medication.frequency}", style = MaterialTheme.typography.bodySmall)
                medication.scheduled_times?.firstOrNull()?.let { time ->
                    Text("Time: $time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            medication.special_instructions?.takeIf { it.isNotBlank() }?.let { instructions ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MedicationScheduleCard(todaysMedications: List<Medication>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("💊", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Medication Schedule (Today)", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))
            if (todaysMedications.isEmpty()) {
                Text(
                    "No medications scheduled for today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    todaysMedications.forEach { med ->
                        MedicationItemCard(medication = med)
                    }
                }
            }
        }
    }
}


// ─── Shared helper composables ────────────────────────────────────────────────



@Composable

private fun SectionNavCard(

    icon: String,

    title: String,

    subtitle: String,

    isExpanded: Boolean,

    onClick: () -> Unit,

    content: @Composable () -> Unit

) {

    ElevatedCard(

        modifier = Modifier.fillMaxWidth(),

        onClick = onClick

    ) {

        Column {

            Row(

                modifier = Modifier.fillMaxWidth().padding(16.dp),

                verticalAlignment = Alignment.CenterVertically

            ) {

                Surface(

                    modifier = Modifier.size(40.dp),

                    shape = RoundedCornerShape(10.dp),

                    color = MaterialTheme.colorScheme.secondaryContainer

                ) {

                    Box(contentAlignment = Alignment.Center) {

                        Text(icon, style = MaterialTheme.typography.titleMedium)

                    }

                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {

                    Text(title, style = MaterialTheme.typography.titleMedium)

                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                }

                Icon(

                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,

                    contentDescription = null,

                    tint = MaterialTheme.colorScheme.onSurfaceVariant

                )

            }

            if (isExpanded) {

                HorizontalDivider()

                content()

            }

        }

    }

}



@Composable

fun PetCard(

    pet: Pet,

    hasUpcomingAppointments: Boolean = false,

    hasActiveMeds: Boolean = false,

    onClick: () -> Unit

) {

    Card(

        modifier = Modifier.fillMaxWidth().clickable { onClick() }

    ) {

        Row(

            modifier = Modifier.fillMaxWidth().padding(14.dp),

            verticalAlignment = Alignment.CenterVertically

        ) {

            Surface(

                modifier = Modifier.size(52.dp),

                shape = CircleShape,

                color = MaterialTheme.colorScheme.primaryContainer

            ) {

                Box(contentAlignment = Alignment.Center) {

                    Text(

                        text = if (!pet.photo_url.isNullOrBlank()) {

                            pet.photo_url

                        } else {

                            when (pet.species.uppercase()) {

                                "DOG" -> "🐶"

                                "CAT" -> "🐱"

                                else  -> "🐾"

                            }

                        },

                        style = MaterialTheme.typography.headlineMedium

                    )

                }

            }



            Spacer(modifier = Modifier.width(14.dp))



            Column(modifier = Modifier.weight(1f)) {

                Text(pet.name, style = MaterialTheme.typography.titleMedium)

                if (!pet.nickname.isNullOrBlank()) {

                    Text(

                        text = "\"${pet.nickname}\"",

                        style = MaterialTheme.typography.bodySmall,

                        color = MaterialTheme.colorScheme.primary

                    )

                }

                Text(

                    text = buildString {

                        append(pet.species.lowercase().replaceFirstChar { it.uppercase() })

                        pet.breed?.let { append(" • $it") }

                    },

                    style = MaterialTheme.typography.bodySmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant

                )



                if (hasUpcomingAppointments || hasActiveMeds) {

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                        if (hasActiveMeds) {

                            SuggestionChip(

                                onClick = onClick,

                                label = { Text("💊 In Meds", style = MaterialTheme.typography.labelSmall) }

                            )

                        }

                        if (hasUpcomingAppointments) {

                            SuggestionChip(

                                onClick = onClick,

                                label = { Text("📅 Upcoming Visit", style = MaterialTheme.typography.labelSmall) }

                            )

                        }

                    }

                }

            }



            Icon(

                imageVector = Icons.AutoMirrored.Filled.NavigateNext,

                contentDescription = null,

                tint = MaterialTheme.colorScheme.onSurfaceVariant

            )

        }

    }

}



@Composable

fun AddPetDialog(

    ownerId: String,

    isSaving: Boolean,

    onDismiss: () -> Unit,

    onSave: (Pet) -> Unit

) {

    var name by remember { mutableStateOf("") }

    var nickname by remember { mutableStateOf("") }

    var species by remember { mutableStateOf("DOG") }

    var breed by remember { mutableStateOf("") }

    var sex by remember { mutableStateOf("MALE") }

    var birthDate by remember { mutableStateOf("") }

    var weightText by remember { mutableStateOf("") }

    val petIcons = listOf("🐶", "🐕", "🦮", "🐩", "🐱", "🐈", "🐈‍⬛", "🐰", "🐹", "🐾")

    var selectedIcon by remember { mutableStateOf("🐶") }

    var validationError by remember { mutableStateOf("") }



    AlertDialog(

        onDismissRequest = onDismiss,

        title = { Text("Add Pet") },

        text = {

            Column(

                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),

                verticalArrangement = Arrangement.spacedBy(10.dp)

            ) {

                if (validationError.isNotBlank()) {

                    Text(validationError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                }



                Text("Choose Pet Avatar Icon", style = MaterialTheme.typography.labelMedium)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    items(petIcons) { icon ->

                        FilterChip(

                            selected = selectedIcon == icon,

                            onClick = {

                                selectedIcon = icon

                                if (icon in listOf("🐱", "🐈", "🐈‍⬛")) species = "CAT"

                                else if (icon in listOf("🐶", "🐕", "🦮", "🐩")) species = "DOG"

                            },

                            label = { Text(icon, style = MaterialTheme.typography.titleMedium) }

                        )

                    }

                }



                OutlinedTextField(

                    value = name,

                    onValueChange = { name = it; validationError = "" },

                    label = { Text("Pet Name *") },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )



                OutlinedTextField(

                    value = nickname,

                    onValueChange = { nickname = it },

                    label = { Text("Nickname") },

                    placeholder = { Text("e.g. Coco") },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )



                Text("Species", style = MaterialTheme.typography.labelMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    FilterChip(

                        selected = species == "DOG",

                        onClick = { species = "DOG"; if (selectedIcon !in listOf("🐶", "🐕", "🦮", "🐩")) selectedIcon = "🐶" },

                        label = { Text("Dog 🐶") }

                    )

                    FilterChip(

                        selected = species == "CAT",

                        onClick = { species = "CAT"; if (selectedIcon !in listOf("🐱", "🐈", "🐈‍⬛")) selectedIcon = "🐱" },

                        label = { Text("Cat 🐱") }

                    )

                }



                OutlinedTextField(

                    value = breed,

                    onValueChange = { breed = it },

                    label = { Text("Breed") },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )



                Text("Sex", style = MaterialTheme.typography.labelMedium)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    FilterChip(

                        selected = sex == "MALE",

                        onClick = { sex = "MALE" },

                        label = { Text("Male") }

                    )

                    FilterChip(

                        selected = sex == "FEMALE",

                        onClick = { sex = "FEMALE" },

                        label = { Text("Female") }

                    )

                }



                OutlinedTextField(

                    value = birthDate,

                    onValueChange = { birthDate = it },

                    label = { Text("Birthdate") },

                    placeholder = { Text("YYYY-MM-DD") },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )



                OutlinedTextField(

                    value = weightText,

                    onValueChange = { weightText = it },

                    label = { Text("Weight (kg)") },

                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )

            }

        },

        confirmButton = {

            Button(

                onClick = {

                    if (name.isBlank()) {

                        validationError = "Please enter the pet's name."

                        return@Button

                    }

                    val newPet = Pet(

                        owner_id = ownerId,

                        name = name.trim(),

                        nickname = nickname.trim().takeIf { it.isNotBlank() },

                        species = species,

                        breed = breed.trim().takeIf { it.isNotBlank() },

                        sex = sex,

                        birth_date = birthDate.trim().takeIf { it.isNotBlank() },

                        weight = weightText.toDoubleOrNull(),

                        photo_url = selectedIcon

                    )

                    onSave(newPet)

                },

                enabled = !isSaving

            ) {

                if (isSaving) {

                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)

                } else {

                    Text("Add Pet")

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



@Composable

fun EditPetDialog(

    pet: Pet,

    onDismiss: () -> Unit,

    onSave: (Pet) -> Unit,

    isSaving: Boolean

) {

    var name by remember { mutableStateOf(pet.name) }

    var nickname by remember { mutableStateOf(pet.nickname ?: "") }

    var breed by remember { mutableStateOf(pet.breed ?: "") }

    var weightText by remember { mutableStateOf(pet.weight?.toString() ?: "") }

    var allergies by remember { mutableStateOf(pet.known_allergies ?: "") }

    var conditions by remember { mutableStateOf(pet.pre_existing_conditions ?: "") }

    val petIcons = listOf("🐶", "🐕", "🦮", "🐩", "🐱", "🐈", "🐈‍⬛", "🐰", "🐹", "🐾")

    var selectedIcon by remember {

        mutableStateOf(pet.photo_url?.takeIf { it.isNotBlank() } ?: when (pet.species.uppercase()) {
            "DOG" -> "🐶"
            "CAT" -> "🐱"
            else -> "🐾"
        })

    }

    var validationError by remember { mutableStateOf("") }



    AlertDialog(

        onDismissRequest = onDismiss,

        title = { Text("Edit Pet Information") },

        text = {

            Column(

                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),

                verticalArrangement = Arrangement.spacedBy(10.dp)

            ) {

                if (validationError.isNotBlank()) {

                    Text(validationError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)

                }



                Text("Choose Pet Avatar Icon", style = MaterialTheme.typography.labelMedium)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    items(petIcons) { icon ->

                        FilterChip(

                            selected = selectedIcon == icon,

                            onClick = { selectedIcon = icon },

                            label = { Text(icon, style = MaterialTheme.typography.titleMedium) }

                        )

                    }

                }



                OutlinedTextField(

                    value = name,

                    onValueChange = { name = it; validationError = "" },

                    label = { Text("Pet Name *") },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )



                OutlinedTextField(

                    value = nickname,

                    onValueChange = { nickname = it },

                    label = { Text("Nickname") },

                    placeholder = { Text("e.g. Coco") },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )



                OutlinedTextField(

                    value = breed,

                    onValueChange = { breed = it },

                    label = { Text("Breed") },

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )



                OutlinedTextField(

                    value = weightText,

                    onValueChange = { weightText = it },

                    label = { Text("Weight (kg)") },

                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),

                    singleLine = true,

                    modifier = Modifier.fillMaxWidth()

                )



                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Known Allergies") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    readOnly = true
                )



                OutlinedTextField(
                    value = conditions,
                    onValueChange = { conditions = it },
                    label = { Text("Pre-existing Conditions") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    readOnly = true
                )

            }

        },

        confirmButton = {

            Button(

                onClick = {

                    if (name.isBlank()) {

                        validationError = "Pet name cannot be empty."

                        return@Button

                    }

                    val parsedWeight = weightText.toDoubleOrNull()

                    val updatedPet = pet.copy(

                        name = name.trim(),

                        nickname = nickname.trim().takeIf { it.isNotBlank() },

                        breed = breed.trim().takeIf { it.isNotBlank() },

                        weight = parsedWeight,

                        known_allergies = allergies.trim().takeIf { it.isNotBlank() },

                        pre_existing_conditions = conditions.trim().takeIf { it.isNotBlank() },

                        photo_url = selectedIcon

                    )

                    onSave(updatedPet)

                },

                enabled = !isSaving

            ) {

                if (isSaving) {

                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)

                } else {

                    Text("Save")

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

