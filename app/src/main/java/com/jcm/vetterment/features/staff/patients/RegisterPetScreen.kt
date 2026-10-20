package com.jcm.vetterment.features.staff.patients

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.components.DatePickerField
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.navigation.StaffRoutes
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val WEIGHT_REGEX = Regex("^\\d*\\.?\\d*$")

// ─── Logic layer ─────────────────────────────────────────────────────────────

@Composable
fun RegisterPetScreen(
    ownerId: String,
    navController: NavController
) {
    var owner by remember { mutableStateOf<Profile?>(null) }
    var ownerLoadError by remember { mutableStateOf(false) }

    // Form fields
    var name by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var knownAllergies by remember { mutableStateOf("") }
    var preExistingConditions by remember { mutableStateOf("") }

    // UI state
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf("") }

    // Field-level errors
    var nameError by remember { mutableStateOf("") }
    var speciesError by remember { mutableStateOf("") }
    var sexError by remember { mutableStateOf("") }
    var weightError by remember { mutableStateOf("") }
    var birthDateError by remember { mutableStateOf("") }

    LaunchedEffect(ownerId) {
        owner = GetProfileData.getProfileById(ownerId)
        if (owner == null) ownerLoadError = true
    }

    RegisterPetContent(
        owner = owner,
        ownerLoadError = ownerLoadError,

        name = name,
        species = species,
        sex = sex,
        breed = breed,
        birthDate = birthDate,
        weight = weight,
        knownAllergies = knownAllergies,
        preExistingConditions = preExistingConditions,

        nameError = nameError,
        speciesError = speciesError,
        sexError = sexError,
        weightError = weightError,
        birthDateError = birthDateError,
        submitError = submitError,

        isSubmitting = isSubmitting,

        onNameChange = { name = it; nameError = "" },
        onSpeciesChange = { species = it; speciesError = "" },
        onSexChange = { sex = it; sexError = "" },
        onBreedChange = { breed = it },
        onBirthDateChange = { birthDate = it; birthDateError = "" },
        onWeightChange = { input ->
            // Only allow valid numeric characters
            if (input.isEmpty() || input.matches(WEIGHT_REGEX)) {
                weight = input
                weightError = ""
            }
        },
        onKnownAllergiesChange = { knownAllergies = it },
        onPreExistingConditionsChange = { preExistingConditions = it },

        onBackClick = { navController.popBackStack() },

        onSaveClick = {
            // Validate
            var valid = true

            if (name.isBlank()) {
                nameError = "Pet name is required."
                valid = false
            }
            if (species.isBlank()) {
                speciesError = "Species is required."
                valid = false
            }
            if (sex.isBlank()) {
                sexError = "Sex is required."
                valid = false
            }
            if (weight.isNotBlank()) {
                val parsed = weight.toDoubleOrNull()
                if (parsed == null || parsed <= 0) {
                    weightError = "Enter a valid weight."
                    valid = false
                }
            }
            if (birthDate.isNotBlank()) {
                if (!isValidDate(birthDate)) {
                    birthDateError = "Use YYYY-MM-DD format."
                    valid = false
                }
            }

            if (!valid) return@RegisterPetContent

            isSubmitting = true
            submitError = ""
        },
    )

    // Submission effect — runs when isSubmitting flips to true
    LaunchedEffect(isSubmitting) {
        if (!isSubmitting) return@LaunchedEffect

        try {
            val petToCreate = Pet(
                owner_id = ownerId,
                name = name.trim(),
                species = species,
                sex = sex,
                breed = breed.trim().takeIf { it.isNotBlank() },
                birth_date = birthDate.trim().takeIf { it.isNotBlank() },
                weight = weight.trim().toDoubleOrNull(),
                known_allergies = knownAllergies.trim().takeIf { it.isNotBlank() },
                pre_existing_conditions = preExistingConditions.trim().takeIf { it.isNotBlank() },
                photo_url = null
            )

            val created = GetPetData.createPet(petToCreate)

            if (created?.id != null) {
                navController.navigate(StaffRoutes.petDetails(created.id)) {
                    // Remove RegisterPetScreen and SearchOwnerScreen from back stack
                    // so Back from PetDetailsScreen goes straight to PatientsScreen
                    popUpTo(StaffRoutes.SearchOwner) { inclusive = true }
                }
            } else {
                submitError = "Failed to register patient. Please try again."
            }

        } catch (e: Exception) {
            e.printStackTrace()
            submitError = e.message ?: "An unexpected error occurred."
        } finally {
            isSubmitting = false
        }
    }
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterPetContent(
    owner: Profile?,
    ownerLoadError: Boolean,

    name: String,
    species: String,
    sex: String,
    breed: String,
    birthDate: String,
    weight: String,
    knownAllergies: String,
    preExistingConditions: String,

    nameError: String,
    speciesError: String,
    sexError: String,
    weightError: String,
    birthDateError: String,
    submitError: String,

    isSubmitting: Boolean,

    onNameChange: (String) -> Unit,
    onSpeciesChange: (String) -> Unit,
    onSexChange: (String) -> Unit,
    onBreedChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onKnownAllergiesChange: (String) -> Unit,
    onPreExistingConditionsChange: (String) -> Unit,

    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Register Patient",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (owner != null) {
                            Text(
                                text = "Owner: ${owner.full_name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        if (ownerLoadError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Could not load owner information.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Required fields label ─────────────────────────────────────
            Text(
                text = "* Required fields",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Submit error ──────────────────────────────────────────────
            if (submitError.isNotBlank()) {
                Text(
                    text = submitError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── Name * ────────────────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Pet Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError.isNotBlank(),
                supportingText = if (nameError.isNotBlank()) {
                    { Text(nameError) }
                } else null
            )

            // ── Species * ─────────────────────────────────────────────────
            DropdownField(
                label = "Species *",
                selected = species,
                options = listOf("DOG", "CAT"),
                onSelect = onSpeciesChange,
                isError = speciesError.isNotBlank(),
                errorText = speciesError
            )

            // ── Sex * ─────────────────────────────────────────────────────
            DropdownField(
                label = "Sex *",
                selected = sex,
                options = listOf("MALE", "FEMALE", "UNKNOWN"),
                onSelect = onSexChange,
                isError = sexError.isNotBlank(),
                errorText = sexError
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Optional Details",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // ── Breed ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = breed,
                onValueChange = onBreedChange,
                label = { Text("Breed") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // ── Birth Date ────────────────────────────────────────────────
            DatePickerField(
                value = birthDate,
                label = "Birth Date",
                onValueChange = onBirthDateChange,
                errorMessage = birthDateError
            )

            // ── Weight ────────────────────────────────────────────────────
            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Weight (kg)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = weightError.isNotBlank(),
                supportingText = if (weightError.isNotBlank()) {
                    { Text(weightError) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // ── Known Allergies ───────────────────────────────────────────
            OutlinedTextField(
                value = knownAllergies,
                onValueChange = onKnownAllergiesChange,
                label = { Text("Known Allergies") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // ── Pre-existing Conditions ───────────────────────────────────
            OutlinedTextField(
                value = preExistingConditions,
                onValueChange = onPreExistingConditionsChange,
                label = { Text("Pre-existing Conditions") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // ── Photo (coming soon) ───────────────────────────────────────
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pet Photo",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Coming soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Save button ───────────────────────────────────────────────
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving…")
                } else {
                    Text("Save Patient")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // ── Date Picker Dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        PetDatePickerDialog(
            onDateSelected = { selected ->
                onBirthDateChange(selected)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

// ─── Reusable dropdown ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    isError: Boolean = false,
    errorText: String = ""
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            isError = isError,
            supportingText = if (isError && errorText.isNotBlank()) {
                { Text(errorText) }
            } else null
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─── Date Picker Dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        // Restrict to past + today (no future birth dates)
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date = java.time.Instant
                            .ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        onDateSelected(date.format(DATE_FORMATTER))
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun isValidDate(input: String): Boolean {
    return try {
        LocalDate.parse(input.trim(), DATE_FORMATTER)
        true
    } catch (e: DateTimeParseException) {
        false
    }
}
