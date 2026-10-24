package com.jcm.vetterment.features.staff.cases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.features.staff.StaffScaffold
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun CasesScreen(navController: NavController) {
    var cases by remember { mutableStateOf<List<Case>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var pets by remember { mutableStateOf<Map<String, Pet>>(emptyMap()) }
    var vets by remember { mutableStateOf<Map<String, Profile>>(emptyMap()) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = ""
        try {
            val fetchedCases = GetCaseData.getAllCases()
            cases = GetCaseData.getAllCases()

            pets = GetPetData.getPetsByIds(
                cases.map { it.pet_id }.distinct()
            )

            vets = GetProfileData.getProfilesByIds(
                fetchedCases
                    .mapNotNull { it.assigned_vet_id }
                    .distinct()
            )

        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load cases."

        } finally {
            isLoading = false
        }
    }

    CasesContent(
        navController = navController,
        cases = cases,
        pets = pets,
        vets = vets,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onCaseClick = { it.id?.let { id -> navController.navigate(CaseRoutes.caseDetails(id)) } },
        onCreateCaseClick = { navController.navigate(CaseRoutes.SearchPetForCase) }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun CasesContent(
    navController: NavController,
    cases: List<Case>,
    pets: Map<String, Pet>,
    isLoading: Boolean,
    errorMessage: String,
    vets: Map<String, Profile>,
    onCaseClick: (Case) -> Unit,
    onCreateCaseClick: () -> Unit
) {
    val activeCases    = cases.filter { it.current_status !in listOf("COMPLETED", "DISCHARGED") }
    val unassignedCases = activeCases.filter { it.assigned_vet_id == null }
    val assignedCases   = activeCases.filter { it.assigned_vet_id != null }
    val completedCases  = cases.filter { it.current_status in listOf("COMPLETED", "DISCHARGED") }

    StaffScaffold(
        navController = navController,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCaseClick) {
                Icon(Icons.Default.Add, contentDescription = "Create case")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text("Cases", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@StaffScaffold
            }

            if (cases.isEmpty()) {
                Text(
                    "No cases yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@StaffScaffold
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (unassignedCases.isNotEmpty()) {
                    item { CaseSectionHeader(title = "Unassigned", count = unassignedCases.size) }
                    items(unassignedCases) { case ->
                        CaseCard(
                            case = case,
                            pet = pets[case.pet_id],
                            assignedVet = vets[case.assigned_vet_id],
                            onClick = { onCaseClick(case) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                if (assignedCases.isNotEmpty()) {
                    item { CaseSectionHeader(title = "Active", count = assignedCases.size) }
                    items(assignedCases) { case ->
                        CaseCard(
                            case = case,
                            pet = pets[case.pet_id],
                            assignedVet = vets[case.assigned_vet_id],
                            onClick = { onCaseClick(case) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                if (completedCases.isNotEmpty()) {
                    item { CaseSectionHeader(title = "Completed", count = completedCases.size) }
                    items(completedCases) { case ->
                        CaseCard(
                            case = case,
                            pet = pets[case.pet_id],
                            assignedVet = vets[case.assigned_vet_id],
                            onClick = { onCaseClick(case) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun CaseSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CaseCard(
    case: Case,
    pet: Pet?,
    assignedVet: Profile?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = case.case_type.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = pet?.let {
                        val speciesIcon = when (it.species.uppercase()) {
                            "DOG" -> "🐶"
                            "CAT" -> "🐱"
                            else -> "🐾"
                        }
                        "$speciesIcon ${it.name} • ${it.species.lowercase().replaceFirstChar { c -> c.uppercase() }}"
                    } ?: "Unknown Pet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = assignedVet?.let {
                        "Assigned to: ${it.full_name}"
                    } ?: "Unassigned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatusChip(status = case.current_status)
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val isCompleted = status in listOf("COMPLETED", "DISCHARGED")
    val isQueued = status == "QUEUED"
    Surface(
        shape = MaterialTheme.shapes.small,
        color = when {
            isCompleted -> MaterialTheme.colorScheme.tertiaryContainer
            isQueued    -> MaterialTheme.colorScheme.secondaryContainer
            else        -> MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        Text(
            text = CaseStages.displayName(status),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = when {
                isCompleted -> MaterialTheme.colorScheme.onTertiaryContainer
                isQueued    -> MaterialTheme.colorScheme.onSecondaryContainer
                else        -> MaterialTheme.colorScheme.onPrimaryContainer
            }
        )
    }
}
