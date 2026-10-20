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

import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp

import androidx.navigation.NavController

import com.jcm.vetterment.features.data_models.Appointment

import com.jcm.vetterment.features.data_models.Medication

import com.jcm.vetterment.features.data_models.Pet

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



            val cases = GetCaseData.getCasesForPet(petId)

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

                    val success = GetPetData.updatePet(updatedPet)

                    if (success) {

                        pet = updatedPet

                        showEditDialog = false

                    } else {

                        errorMessage = "Unable to update pet details. Please check connection and permissions."

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

        isLoading = isLoading,

        errorMessage = errorMessage,

        onBackClick = { navController.popBackStack() },

        onEditClick = { showEditDialog = true }

    )

}



@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun PetOwnerPetDetailContent(

    pet: Pet?,

    medications: List<Medication>,

    appointments: List<Appointment>,

    isLoading: Boolean,

    errorMessage: String,

    onBackClick: () -> Unit,

    onEditClick: () -> Unit = {}

) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = { Text(pet?.name ?: "Pet Details") },

                navigationIcon = {

                    IconButton(onClick = onBackClick) {

                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")

                    }

                },

                actions = {

                    if (pet != null) {

                        IconButton(onClick = onEditClick) {

                            Icon(Icons.Filled.Edit, "Edit Pet")

                        }

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

                    onEditClick = onEditClick

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

    onEditClick: () -> Unit

) {

    var expandedSection by remember { mutableStateOf<String?>("basic") }



    Column(

        modifier = Modifier

            .fillMaxSize()

            .verticalScroll(rememberScrollState())

            .padding(16.dp),

        verticalArrangement = Arrangement.spacedBy(16.dp)

    ) {

        // ── Identity Card (matching mockup) ──────────────────────────────

        Card(modifier = Modifier.fillMaxWidth()) {

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

                                        else  -> "🐾"

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

                        Text(

                            text = buildString {

                                pet.breed?.let { append(it) } ?: append(pet.species.lowercase().replaceFirstChar { it.uppercase() })

                                pet.birth_date?.let { append(" • $it") }

                            },

                            style = MaterialTheme.typography.bodyMedium,

                            color = MaterialTheme.colorScheme.onSurfaceVariant

                        )

                        pet.weight?.let {

                            Text(

                                text = "${it} kg",

                                style = MaterialTheme.typography.bodySmall,

                                color = MaterialTheme.colorScheme.onSurfaceVariant

                            )

                        }

                    }

                    IconButton(onClick = onEditClick) {

                        Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")

                    }

                }

            }

        }



        // ── Navigation Section Rows (matching mockup) ─────────────────────



        // Section 1: Vaccinations

        SectionNavCard(

            icon = "💉",

            title = "Vaccinations",

            subtitle = "Vaccination history and upcoming due dates.",

            isExpanded = expandedSection == "vaccinations",

            onClick = {

                expandedSection = if (expandedSection == "vaccinations") null else "vaccinations"

            }

        ) {

            Column(

                modifier = Modifier.padding(16.dp),

                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                Text(

                    text = "No upcoming vaccinations recorded.",

                    style = MaterialTheme.typography.bodyMedium,

                    color = MaterialTheme.colorScheme.onSurfaceVariant

                )

                Text(

                    text = "Vaccination records verified by clinic staff will appear here.",

                    style = MaterialTheme.typography.bodySmall,

                    color = MaterialTheme.colorScheme.onSurfaceVariant

                )

            }

        }



        // Section 2: Medications

        SectionNavCard(

            icon = "💊",

            title = "Medications",

            subtitle = "Current prescriptions and their schedules, if any.",

            isExpanded = expandedSection == "medications",

            onClick = {

                expandedSection = if (expandedSection == "medications") null else "medications"

            }

        ) {

            Column(

                modifier = Modifier.padding(16.dp),

                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                if (medications.isEmpty()) {

                    Text(

                        text = "No active prescriptions.",

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                } else {

                    medications.forEach { med ->

                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {

                            Column(modifier = Modifier.padding(12.dp)) {

                                Text(med.name, style = MaterialTheme.typography.titleSmall)

                                Text("${med.dosage} • ${med.frequency}", style = MaterialTheme.typography.bodySmall)

                                med.end_date?.let {

                                    Text("Until $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                }

                            }

                        }

                    }

                }

            }

        }



        // Section 3: Appointment History

        SectionNavCard(

            icon = "📋",

            title = "Appointment History",

            subtitle = "Previous visits, approved medical records, and 3D visualizations.",

            isExpanded = expandedSection == "appointments",

            onClick = {

                expandedSection = if (expandedSection == "appointments") null else "appointments"

            }

        ) {

            Column(

                modifier = Modifier.padding(16.dp),

                verticalArrangement = Arrangement.spacedBy(8.dp)

            ) {

                if (appointments.isEmpty()) {

                    Text(

                        text = "No past appointments booked.",

                        style = MaterialTheme.typography.bodyMedium,

                        color = MaterialTheme.colorScheme.onSurfaceVariant

                    )

                } else {

                    appointments.forEach { appt ->

                        OutlinedCard(modifier = Modifier.fillMaxWidth()) {

                            Row(

                                modifier = Modifier.fillMaxWidth().padding(12.dp),

                                horizontalArrangement = Arrangement.SpaceBetween,

                                verticalAlignment = Alignment.CenterVertically

                            ) {

                                Column {

                                    Text(

                                        text = appt.appointment_type.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },

                                        style = MaterialTheme.typography.titleSmall

                                    )

                                    Text(

                                        text = "${appt.appointment_date} at ${appt.appointment_time.take(5)}",

                                        style = MaterialTheme.typography.bodySmall,

                                        color = MaterialTheme.colorScheme.onSurfaceVariant

                                    )

                                }

                                AssistChip(

                                    onClick = {},

                                    label = { Text(appt.status.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }

                                )

                            }

                        }

                    }

                }

            }

        }



        Spacer(modifier = Modifier.height(24.dp))

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

        mutableStateOf(pet.photo_url?.takeIf { it.isNotBlank() } ?: if (pet.species.uppercase() == "DOG") "🐶" else "🐱")

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

                    maxLines = 3

                )



                OutlinedTextField(

                    value = conditions,

                    onValueChange = { conditions = it },

                    label = { Text("Pre-existing Conditions") },

                    modifier = Modifier.fillMaxWidth(),

                    maxLines = 3

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

