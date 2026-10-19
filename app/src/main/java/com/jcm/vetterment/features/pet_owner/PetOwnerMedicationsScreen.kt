package com.jcm.vetterment.features.pet_owner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Medication
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetMedicationData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

// ── Screen (logic layer) ──────────────────────────────────────────────────────

@Composable
fun PetOwnerMedicationsScreen(navController: NavController) {
    // List of (medication, pet) pairs, grouped for display
    var medicationsByPet by remember { mutableStateOf<Map<Pet?, List<Medication>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            if (currentUser == null) {
                errorMessage = "Not signed in."
                return@LaunchedEffect
            }

            // 1. Fetch all pets belonging to the owner
            val pets = GetPetData.getPetsForOwner(currentUser.id)

            // 2. For each pet, fetch active cases, then medications per case
            val result = mutableMapOf<Pet?, MutableList<Medication>>()
            pets.forEach { pet ->
                val cases = GetCaseData.getCasesForPet(pet.id ?: return@forEach)
                cases.forEach { case ->
                    val meds = GetMedicationData.getMedicationsForCase(case.id ?: return@forEach)
                    val activeMeds = meds.filter { it.is_active }
                    if (activeMeds.isNotEmpty()) {
                        result.getOrPut(pet) { mutableListOf() }.addAll(activeMeds)
                    }
                }
            }
            medicationsByPet = result
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Failed to load medications."
        } finally {
            isLoading = false
        }
    }

    PetOwnerMedicationsContent(
        navController = navController,
        medicationsByPet = medicationsByPet,
        isLoading = isLoading,
        errorMessage = errorMessage
    )
}

// ── Content (UI layer) ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PetOwnerMedicationsContent(
    navController: NavController,
    medicationsByPet: Map<Pet?, List<Medication>>,
    isLoading: Boolean,
    errorMessage: String?
) {
    PetOwnerScaffold(navController = navController) { paddingValues ->
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Medications") })
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(innerPadding)
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.Center)
                                .padding(16.dp)
                        )
                    }

                    medicationsByPet.isEmpty() -> {
                        Text(
                            text = "No active medications.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.Center)
                                .padding(16.dp)
                        )
                    }

                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            medicationsByPet.forEach { (pet, meds) ->
                                // Pet name header
                                item {
                                    Text(
                                        text = pet?.name ?: "Unknown Pet",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    )
                                }
                                items(meds) { med ->
                                    OwnerMedicationCard(medication = med, pet = pet)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helper composables ────────────────────────────────────────────────────────

@Composable
fun OwnerMedicationCard(medication: Medication, pet: Pet?) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = medication.name,
                style = MaterialTheme.typography.titleMedium
            )
            if (pet != null) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${medication.dosage} • ${medication.frequency}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!medication.scheduled_times.isNullOrEmpty()) {
                Text(
                    text = medication.scheduled_times.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!medication.end_date.isNullOrBlank()) {
                Text(
                    text = "Until ${medication.end_date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!medication.special_instructions.isNullOrBlank()) {
                Text(
                    text = medication.special_instructions,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
