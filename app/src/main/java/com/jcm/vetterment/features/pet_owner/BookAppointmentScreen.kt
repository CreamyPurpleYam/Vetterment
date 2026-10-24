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

    // Form state - only Consultation type allowed
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var date by remember { mutableStateOf("") }
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
        date = date,
        time = time,
        reason = reason,
        onPetSelect = { selectedPet = it },
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
            if (date.isBlank()) {
                errorMessage = "Please select a date."
                return@BookAppointmentContent
            }
            if (time.isBlank()) {
                errorMessage = "Please select a preferred time."
                return@BookAppointmentContent
            }

            isSubmitting = true
            errorMessage = ""
            coroutineScope.launch {
                try {
                    val appt = Appointment(
                        pet_id = pet.id,
                        owner_id = ownerId,
                        appointment_type = "CONSULTATION",
                        appointment_date = date.trim(),
                        appointment_time = "${time.trim()}:00",
                        reason = reason.trim().takeIf { it.isNotBlank() }
                    )
                    val result = GetAppointmentData.createAppointment(appt)
                    if (result != null) {
                        successMessage = "Consultation request submitted. The clinic will review it shortly."
                    } else {
                        errorMessage = "Failed to submit request. Please check your inputs."
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentContent(
    pets: List<Pet>,
    isLoadingPets: Boolean,
    isSubmitting: Boolean,
    errorMessage: String,
    successMessage: String,
    selectedPet: Pet?,
    date: String,
    time: String,
    reason: String,
    onPetSelect: (Pet) -> Unit,
    onDateChange: (String) -> Unit,
    onTimeChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSubmit: () -> Unit
) {
    var petMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Consultation") },
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

            // ── Appointment Type (Fixed: Consultation) ────────────────────
            Text("Appointment Type", style = MaterialTheme.typography.labelLarge)
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Consultation", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Check-up",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Doctor Fee ────────────────────────────────────────────────
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Doctor's Fee", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("₱400", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(
                        "Payable at clinic",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // ── Date and time ─────────────────────────────────────────────
            Text("Preferred Schedule", style = MaterialTheme.typography.labelLarge)

            DatePickerField(
                value = date,
                label = "Visit Date",
                onValueChange = onDateChange
            )

            TimePickerField(
                value = time,
                label = "Preferred Time (not guaranteed)",
                onValueChange = onTimeChange
            )

            // ── Booking Policy Notice ─────────────────────────────────────
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("⚠ ", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Booking Policy",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "Consultation appointments are for planning your visit and do not guarantee a specific consultation time or queue priority. PetHub Veterinary Clinic operates 24/7 and follows a first-come-first-served policy. For emergencies, visit or contact the clinic directly instead of waiting for an app appointment.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

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
                    Text("Submit Request")
                } else {
                    Text("Submit Request")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

