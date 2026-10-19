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
import com.jcm.vetterment.features.components.DatePickerField
import com.jcm.vetterment.features.components.TimePickerField
import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.supabase.GetAppointmentData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData
import kotlinx.coroutines.launch

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun BookAppointmentScreen(navController: NavController) {

    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var ownerId by remember { mutableStateOf("") }
    var isLoadingPets by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    // Form state
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var selectedType by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var appointmentDate by remember { mutableStateOf("") }
    var appointmentTime by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val profile = GetProfileData.getCurrentProfile() ?: return@LaunchedEffect
            ownerId = profile.id
            pets = GetPetData.getPetsForOwner(profile.id)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load your pets."
        } finally {
            isLoadingPets = false
        }
    }

    BookAppointmentContent(
        pets = pets,
        isLoadingPets = isLoadingPets,
        isSubmitting = isSubmitting,
        errorMessage = errorMessage,
        successMessage = successMessage,
        selectedPet = selectedPet,
        selectedType = selectedType,
        date = date,
        time = time,
        reason = reason,
        onPetSelect = { selectedPet = it },
        onTypeSelect = { selectedType = it },
        onDateChange = { date = it },
        onTimeChange = { time = it },
        onReasonChange = { reason = it },
        onBackClick = { navController.popBackStack() },
        onSubmit = {
            val pet = selectedPet
            if (pet?.id == null) {
                errorMessage = "Please select a pet."
                return@BookAppointmentContent
            }
            if (selectedType.isBlank()) {
                errorMessage = "Please select an appointment type."
                return@BookAppointmentContent
            }
            if (date.isBlank()) {
                errorMessage = "Please enter a date (YYYY-MM-DD)."
                return@BookAppointmentContent
            }
            if (time.isBlank()) {
                errorMessage = "Please enter a time (HH:MM)."
                return@BookAppointmentContent
            }

            isSubmitting = true
            errorMessage = ""
            coroutineScope.launch {
                try {
                    val appt = Appointment(
                        pet_id = pet.id,
                        owner_id = ownerId,
                        appointment_type = selectedType,
                        appointment_date = date.trim(),
                        appointment_time = "${time.trim()}:00",
                        reason = reason.trim().takeIf { it.isNotBlank() }
                    )
                    val result = GetAppointmentData.createAppointment(appt)
                    if (result != null) {
                        successMessage = "Appointment request submitted. The clinic will review it shortly."
                    } else {
                        errorMessage = "Failed to submit appointment. Please check your inputs."
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    errorMessage = "Something went wrong. Please try again."
                } finally {
                    isSubmitting = false
                }
            }
        }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

private val appointmentTypes = listOf(
    "CONSULTATION",
    "VACCINATION",
    "FOLLOW_UP",
    "SURGERY",
    "GROOMING",
    "EMERGENCY",
    "OTHER"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentContent(
    pets: List<Pet>,
    isLoadingPets: Boolean,
    isSubmitting: Boolean,
    errorMessage: String,
    successMessage: String,
    selectedPet: Pet?,
    selectedType: String,
    date: String,
    time: String,
    reason: String,
    onPetSelect: (Pet) -> Unit,
    onTypeSelect: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSubmit: () -> Unit
) {
    var petMenuExpanded by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Appointment") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        if (successMessage.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = successMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBackClick) {
                        Text("Back to Appointments")
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }

            // ── Pet selector ──────────────────────────────────────────────
            Text("Select Pet", style = MaterialTheme.typography.labelLarge)

            if (isLoadingPets) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                ExposedDropdownMenuBox(
                    expanded = petMenuExpanded,
                    onExpandedChange = { petMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedPet?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pet") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = petMenuExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = petMenuExpanded,
                        onDismissRequest = { petMenuExpanded = false }
                    ) {
                        pets.forEach { pet ->
                            DropdownMenuItem(
                                text = { Text(pet.name) },
                                onClick = {
                                    onPetSelect(pet)
                                    petMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Appointment type ──────────────────────────────────────────
            Text("Appointment Type", style = MaterialTheme.typography.labelLarge)

            ExposedDropdownMenuBox(
                expanded = typeMenuExpanded,
                onExpandedChange = { typeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedType
                        .lowercase()
                        .replace("_", " ")
                        .replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = typeMenuExpanded,
                    onDismissRequest = { typeMenuExpanded = false }
                ) {
                    appointmentTypes.forEach { type ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    type.lowercase()
                                        .replace("_", " ")
                                        .replaceFirstChar { it.uppercase() }
                                )
                            },
                            onClick = {
                                onTypeSelect(type)
                                typeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Date and time ─────────────────────────────────────────────
            Text("Preferred Schedule", style = MaterialTheme.typography.labelLarge)

            DatePickerField(
                value = date,
                label = "Date",
                onValueChange = onDateChange
            )

            TimePickerField(
                value = time,
                label = "Time",
                onValueChange = onTimeChange
            )
            // ── Reason ────────────────────────────────────────────────────
            Text("Reason / Notes", style = MaterialTheme.typography.labelLarge)

            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChange,
                label = { Text("Reason (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSubmit,
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
                    Text("Submitting…")
                } else {
                    Text("Submit Request")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

