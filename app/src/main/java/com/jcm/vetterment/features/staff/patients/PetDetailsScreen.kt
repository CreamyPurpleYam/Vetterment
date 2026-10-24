package com.jcm.vetterment.features.staff.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.features.staff.cases.CaseStages
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.TextButton

@Composable
fun PetDetailsScreen(
    petId: String,
    navController: NavController
) {
    var pet by remember { mutableStateOf<Pet?>(null) }
    var owner by remember { mutableStateOf<Profile?>(null) }
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
            val fetchedPet = GetPetData.getPetById(petId)
            if (fetchedPet == null) {
                errorMessage = "Patient not found."
            } else {
                pet = fetchedPet
                owner = GetProfileData.getProfileById(fetchedPet.owner_id)
                cases = GetCaseData.getCasesForPet(petId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load patient details."
        } finally {
            isLoading = false
        }
    }

    if (showEditDialog && pet != null) {
        EditPetDialog(
            pet = pet!!,
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
            },
            isSaving = isSaving
        )
    }

    PetDetailsContent(
        pet = pet,
        owner = owner,
        cases = cases,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBackClick = { navController.popBackStack() },
        onCaseClick = { navController.navigate(CaseRoutes.caseDetails(it.id!!)) },
        onEditClick = { showEditDialog = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetDetailsContent(
    pet: Pet?,
    owner: Profile?,
    cases: List<Case>,
    isLoading: Boolean,
    errorMessage: String,
    onBackClick: () -> Unit,
    onCaseClick: (Case) -> Unit,
    onEditClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pet?.name ?: "Patient Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (pet != null) {
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Patient"
                            )
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
                pet != null -> {
                    PetDetailsBody(
                        pet = pet,
                        owner = owner,
                        cases = cases,
                        onCaseClick = onCaseClick
                    )
                }
            }
        }
    }
}

@Composable
private fun PetDetailsBody(
    pet: Pet,
    owner: Profile?,
    cases: List<Case>,
    onCaseClick: (Case) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Species icon + name header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (pet.species.uppercase() == "DOG") "🐶" else "🐱",
                style = MaterialTheme.typography.displaySmall
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${pet.species.lowercase().replaceFirstChar { it.uppercase() }}" +
                            (pet.breed?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        DetailSection(title = "Pet Profile") {
            DetailRow("Sex", pet.sex.lowercase().replaceFirstChar { it.uppercase() })
            DetailRow("Birth Date", pet.birth_date ?: "Not recorded")
            DetailRow("Weight", pet.weight?.let { "$it kg" } ?: "Not recorded")
            DetailRow("Status", if (pet.is_active) "Active" else "Inactive")
        }

        HorizontalDivider()

        DetailSection(title = "Medical Information") {
            DetailRow(
                "Known Allergies",
                pet.known_allergies?.takeIf { it.isNotBlank() } ?: "None recorded"
            )
            DetailRow(
                "Pre-existing Conditions",
                pet.pre_existing_conditions?.takeIf { it.isNotBlank() } ?: "None recorded"
            )
        }

        HorizontalDivider()

        DetailSection(title = "Owner") {
            if (owner != null) {
                DetailRow("Name", owner.full_name)
                DetailRow("Email", owner.email)
            } else {
                Text(
                    text = "Owner info unavailable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider()

        // Case history
        DetailSection(title = "Case History") {
            if (cases.isEmpty()) {
                Text(
                    text = "No cases on record.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                cases.forEach { case ->
                    CaseHistoryItem(case = case, onClick = { onCaseClick(case) })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CaseHistoryItem(case: Case, onClick: () -> Unit) {
    val isCompleted = case.current_status in listOf("COMPLETED", "DISCHARGED")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = case.case_type.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = CaseStages.displayName(case.current_status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            color = if (isCompleted)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = if (isCompleted) "Completed" else "Active",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                color = if (isCompleted)
                    MaterialTheme.colorScheme.onTertiaryContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun DetailSection(
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
private fun DetailRow(label: String, value: String) {
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

// ─── Edit Pet Dialog for Staff ─────────────────────────────────────────────────

@Composable
fun EditPetDialog(
    pet: Pet,
    onDismiss: () -> Unit,
    onSave: (Pet) -> Unit,
    isSaving: Boolean
) {
    var name by remember { mutableStateOf(pet.name) }
    var nickname by remember { mutableStateOf(pet.nickname ?: "") }
    var species by remember { mutableStateOf(pet.species) }
    var breed by remember { mutableStateOf(pet.breed ?: "") }
    var sex by remember { mutableStateOf(pet.sex) }
    var birthDate by remember { mutableStateOf(pet.birth_date ?: "") }
    var weightText by remember { mutableStateOf(pet.weight?.toString() ?: "") }
    var knownAllergies by remember { mutableStateOf(pet.known_allergies ?: "") }
    var preExistingConditions by remember { mutableStateOf(pet.pre_existing_conditions ?: "") }
    var photoUrl by remember { mutableStateOf(pet.photo_url ?: "") }
    var isActive by remember { mutableStateOf(pet.is_active) }
    var validationError by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Edit Patient Information") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (validationError.isNotBlank()) {
                    Text(validationError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // Pet Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; validationError = "" },
                    label = { Text("Pet Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Nickname
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Nickname") },
                    placeholder = { Text("e.g. Coco") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Species
                Text("Species", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = species == "DOG",
                        onClick = { species = "DOG" },
                        label = { Text("Dog 🐶") }
                    )
                    FilterChip(
                        selected = species == "CAT",
                        onClick = { species = "CAT" },
                        label = { Text("Cat 🐱") }
                    )
                }

                // Sex
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
                    FilterChip(
                        selected = sex == "UNKNOWN",
                        onClick = { sex = "UNKNOWN" },
                        label = { Text("Unknown") }
                    )
                }

                // Breed
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("Breed") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Birth Date
                OutlinedTextField(
                    value = birthDate,
                    onValueChange = { birthDate = it },
                    label = { Text("Birth Date (YYYY-MM-DD)") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Weight
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Photo URL
                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text("Photo URL / Emoji") },
                    placeholder = { Text("e.g. 🐶 or https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Known Allergies (editable by staff)
                OutlinedTextField(
                    value = knownAllergies,
                    onValueChange = { knownAllergies = it },
                    label = { Text("Known Allergies") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // Pre-existing Conditions (editable by staff)
                OutlinedTextField(
                    value = preExistingConditions,
                    onValueChange = { preExistingConditions = it },
                    label = { Text("Pre-existing Conditions") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                // Active status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    androidx.compose.material3.Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                    Text("Active Patient", style = MaterialTheme.typography.bodyMedium)
                }
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
                        species = species,
                        breed = breed.trim().takeIf { it.isNotBlank() },
                        sex = sex,
                        birth_date = birthDate.trim().takeIf { it.isNotBlank() },
                        weight = parsedWeight,
                        known_allergies = knownAllergies.trim().takeIf { it.isNotBlank() },
                        pre_existing_conditions = preExistingConditions.trim().takeIf { it.isNotBlank() },
                        photo_url = photoUrl.trim().takeIf { it.isNotBlank() },
                        is_active = isActive
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
