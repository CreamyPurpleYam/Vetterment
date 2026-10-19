package com.jcm.vetterment.features.pet_owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.staff.cases.CaseStages
import com.jcm.vetterment.navigation.PetOwnerRoutes
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun PetOwnerCasesScreen(navController: NavController) {

    var casesWithPets by remember { mutableStateOf<List<Pair<Case, Pet?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = ""
        try {
            val profile = GetProfileData.getCurrentProfile() ?: run {
                errorMessage = "Unable to load profile."
                return@LaunchedEffect
            }
            val pets = GetPetData.getPetsForOwner(profile.id)
            val result = mutableListOf<Pair<Case, Pet?>>()
            pets.forEach { pet ->
                val petId = pet.id ?: return@forEach
                GetCaseData.getCasesForPet(petId).forEach { case ->
                    result.add(case to pet)
                }
            }
            // Most recent first
            casesWithPets = result.sortedByDescending { it.first.created_at }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load cases."
        } finally {
            isLoading = false
        }
    }

    PetOwnerCasesContent(
        navController = navController,
        casesWithPets = casesWithPets,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onCaseClick = { navController.navigate(PetOwnerRoutes.caseDetail(it.id!!)) }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun PetOwnerCasesContent(
    navController: NavController,
    casesWithPets: List<Pair<Case, Pet?>>,
    isLoading: Boolean,
    errorMessage: String,
    onCaseClick: (Case) -> Unit
) {
    val activeCases = casesWithPets.filter {
        it.first.current_status !in listOf("COMPLETED", "DISCHARGED", "CANCELLED")
    }
    val completedCases = casesWithPets.filter {
        it.first.current_status in listOf("COMPLETED", "DISCHARGED")
    }

    PetOwnerScaffold(navController = navController) { innerPadding ->
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
                return@PetOwnerScaffold
            }

            if (casesWithPets.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No cases yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@PetOwnerScaffold
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (activeCases.isNotEmpty()) {
                    item {
                        PetOwnerCaseSectionHeader("Active", activeCases.size)
                    }
                    items(activeCases) { (case, pet) ->
                        PetOwnerCaseCard(
                            case = case,
                            pet = pet,
                            onClick = { onCaseClick(case) }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                }

                if (completedCases.isNotEmpty()) {
                    item {
                        PetOwnerCaseSectionHeader("Completed", completedCases.size)
                    }
                    items(completedCases) { (case, pet) ->
                        PetOwnerCaseCard(
                            case = case,
                            pet = pet,
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
private fun PetOwnerCaseSectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
fun PetOwnerCaseCard(case: Case, pet: Pet?, onClick: () -> Unit) {
    val isCompleted = case.current_status in listOf("COMPLETED", "DISCHARGED")

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
            Text(
                text = when (pet?.species?.uppercase()) {
                    "DOG" -> "🐶"
                    "CAT" -> "🐱"
                    else  -> "🐾"
                },
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pet?.name ?: "Unknown Pet",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = case.case_type
                        .lowercase()
                        .replace("_", " ")
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CaseStages.displayName(case.current_status),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCompleted)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
