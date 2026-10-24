package com.jcm.vetterment.features.branchmanager

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
import com.jcm.vetterment.features.data_models.PatientListItem
import com.jcm.vetterment.navigation.StaffRoutes
import com.jcm.vetterment.supabase.GetPetData
import kotlinx.coroutines.delay

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun BranchManagerPatientsScreen(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    var patients by remember { mutableStateOf<List<PatientListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(searchText) {
        val normalizedSearch = searchText.trim()
        isLoading = true
        if (normalizedSearch.isNotBlank()) delay(300)
        errorMessage = ""
        try {
            val raw = GetPetData.getPatientList(normalizedSearch)
            patients = sortPatients(raw)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load patients."
            patients = emptyList()
        } finally {
            isLoading = false
        }
    }

    BranchManagerPatientsContent(
        navController = navController,
        searchText = searchText,
        patients = patients,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onSearchChange = { searchText = it },
        onPatientClick = { navController.navigate(StaffRoutes.petDetails(it.pet_id)) },
        onRegisterPatientClick = { navController.navigate(StaffRoutes.SearchOwner) }
    )
}

// ─── Sort helper ──────────────────────────────────────────────────────────────

private fun sortPatients(patients: List<PatientListItem>): List<PatientListItem> {
    return patients.sortedWith(
        compareBy<PatientListItem> { it.active_status.lowercase() != "active" }
            .thenBy { it.pet_name.lowercase() }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun BranchManagerPatientsContent(
    navController: NavController,
    searchText: String,
    patients: List<PatientListItem>,
    isLoading: Boolean,
    errorMessage: String,
    onSearchChange: (String) -> Unit,
    onPatientClick: (PatientListItem) -> Unit,
    onRegisterPatientClick: () -> Unit
) {
    BranchManagerScaffold(
        navController = navController,
        floatingActionButton = {
            FloatingActionButton(onClick = onRegisterPatientClick) {
                Icon(Icons.Default.Add, contentDescription = "Register patient")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text("Patients", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search patient or owner") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@BranchManagerScaffold
            }

            if (patients.isEmpty()) {
                Text("No patients found.")
                return@BranchManagerScaffold
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
            ) {
                items(patients) { patient ->
                    PatientCard(
                        patient = patient,
                        onClick = { onPatientClick(patient) }
                    )
                }
            }
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
fun PatientCard(patient: PatientListItem, onClick: () -> Unit) {
    val status = patient.active_status

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
                text = if (patient.species.uppercase() == "DOG") "🐶" else "🐱",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(patient.pet_name, style = MaterialTheme.typography.titleMedium)
                Text(
                    patient.breed ?: "No breed",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Owner: ${patient.owner_name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = when (status.lowercase()) {
                    "active"    -> MaterialTheme.colorScheme.primary
                    "completed" -> MaterialTheme.colorScheme.tertiary
                    else        -> MaterialTheme.colorScheme.secondary
                }
            )
        }
    }
}