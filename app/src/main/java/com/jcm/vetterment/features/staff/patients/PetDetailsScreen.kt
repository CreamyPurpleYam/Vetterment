package com.jcm.vetterment.features.staff.patients

import androidx.compose.foundation.clickable
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
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData

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

    PetDetailsContent(
        pet = pet,
        owner = owner,
        cases = cases,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBackClick = { navController.popBackStack() },
        onCaseClick = { navController.navigate(CaseRoutes.caseDetails(it.id!!)) }
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
    onCaseClick: (Case) -> Unit
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
