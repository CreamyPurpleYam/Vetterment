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
import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.features.staff.cases.CaseStages
import com.jcm.vetterment.features.staff.cases.CaseTimeline
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun PetOwnerCaseDetailScreen(
    caseId: String,
    navController: NavController
) {
    var case by remember { mutableStateOf<Case?>(null) }
    var pet by remember { mutableStateOf<Pet?>(null) }
    var assignedVet by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(caseId) {
        isLoading = true
        errorMessage = ""
        try {
            val fetchedCase = GetCaseData.getCaseById(caseId) ?: run {
                errorMessage = "Case not found."
                return@LaunchedEffect
            }
            case = fetchedCase
            pet = GetPetData.getPetById(fetchedCase.pet_id)
            assignedVet = fetchedCase.assigned_vet_id?.let {
                GetProfileData.getProfileById(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load case."
        } finally {
            isLoading = false
        }
    }

    PetOwnerCaseDetailContent(
        case = case,
        pet = pet,
        assignedVet = assignedVet,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBackClick = { navController.popBackStack() }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetOwnerCaseDetailContent(
    case: Case?,
    pet: Pet?,
    assignedVet: Profile?,
    isLoading: Boolean,
    errorMessage: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = case?.case_type
                                ?.lowercase()
                                ?.replaceFirstChar { it.uppercase() } ?: "Case Details",
                            style = MaterialTheme.typography.titleLarge
                        )
                        pet?.let {
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
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
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                errorMessage.isNotBlank() -> Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
                case != null -> PetOwnerCaseDetailBody(
                    case = case,
                    pet = pet,
                    assignedVet = assignedVet
                )
            }
        }
    }
}

// ─── Body ─────────────────────────────────────────────────────────────────────

@Composable
private fun PetOwnerCaseDetailBody(
    case: Case,
    pet: Pet?,
    assignedVet: Profile?
) {
    val stages = CaseStages.stagesFor(case.case_type)
    val currentIndex = stages.indexOf(case.current_status)
    val isCompleted = case.current_status in listOf("COMPLETED", "DISCHARGED")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Pet summary card ──────────────────────────────────────────────
        if (pet != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (pet.species.uppercase()) {
                            "DOG" -> "🐶"
                            "CAT" -> "🐱"
                            else  -> "🐾"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(pet.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = pet.breed ?: pet.species.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        assignedVet?.let {
                            Text(
                                text = "Veterinarian: Dr. ${it.full_name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: Text(
                            text = "Veterinarian: Not yet assigned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Timeline ──────────────────────────────────────────────────────
        HorizontalDivider()
        Text(
            text = "Status",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        CaseTimeline(stages = stages, currentIndex = currentIndex)

        // ── Case info — owner-safe fields only ────────────────────────────
        HorizontalDivider()
        Text(
            text = "Visit Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )

        CaseOwnerRow(
            "Type",
            case.case_type.lowercase()
                .replace("_", " ")
                .replaceFirstChar { it.uppercase() }
        )
        CaseOwnerRow("Status", CaseStages.displayName(case.current_status))
        case.admission_date?.let { CaseOwnerRow("Admission Date", it) }
        case.expected_discharge_date?.let { CaseOwnerRow("Expected Discharge", it) }
        case.actual_discharge_date?.let { CaseOwnerRow("Discharge Date", it) }

        // ── Discharge summary — visible to owner once completed ───────────
        if (isCompleted) {
            case.discharge_summary?.takeIf { it.isNotBlank() }?.let { summary ->
                HorizontalDivider()
                Text(
                    text = "Discharge Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ── Status message ────────────────────────────────────────────────
        if (!isCompleted) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when {
                        assignedVet == null ->
                            "Your pet is in the queue. A veterinarian will be assigned shortly."
                        else ->
                            "Your pet is currently under the care of Dr. ${assignedVet.full_name}."
                    },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun CaseOwnerRow(label: String, value: String) {
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
