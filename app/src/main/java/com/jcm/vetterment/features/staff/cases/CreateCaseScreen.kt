package com.jcm.vetterment.features.staff.cases

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.PatientListItem
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetPetData
import kotlinx.coroutines.delay

// ─── Step 1: Search for a pet ─────────────────────────────────────────────────

@Composable
fun SearchPetForCaseScreen(navController: NavController) {
    var searchText by remember { mutableStateOf("") }
    var patients by remember { mutableStateOf<List<PatientListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    SearchPetForCaseContent(
        searchText = searchText,
        patients = patients,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onSearchChange = { searchText = it },
        onPatientClick = { patient ->
            navController.navigate(CaseRoutes.selectCaseType(patient.pet_id))
        },
        onBackClick = { navController.popBackStack() }
    )

    LaunchedEffect(searchText) {
        val normalized = searchText.trim()
        if (normalized.isBlank()) {
            patients = emptyList()
            isLoading = false
            return@LaunchedEffect
        }
        delay(300)
        isLoading = true
        errorMessage = ""
        try {
            patients = GetPetData.getPatientList(normalized)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to search patients."
            patients = emptyList()
        } finally {
            isLoading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchPetForCaseContent(
    searchText: String,
    patients: List<PatientListItem>,
    isLoading: Boolean,
    errorMessage: String,
    onSearchChange: (String) -> Unit,
    onPatientClick: (PatientListItem) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Patient") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
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

            when {
                isLoading -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                searchText.isBlank() -> {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Search for the patient to create a case for.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                patients.isEmpty() -> {
                    Text(
                        text = "No patients found for \"$searchText\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(patients) { patient ->
                            PatientPickerCard(
                                patient = patient,
                                onClick = { onPatientClick(patient) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PatientPickerCard(patient: PatientListItem, onClick: () -> Unit) {
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
        }
    }
}

// ─── Step 2: Select case type and create ─────────────────────────────────────

@Composable
fun SelectCaseTypeScreen(
    petId: String,
    navController: NavController
) {
    var selectedType by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    SelectCaseTypeContent(
        selectedType = selectedType,
        isSubmitting = isSubmitting,
        errorMessage = errorMessage,
        onTypeSelect = { selectedType = it },
        onBackClick = { navController.popBackStack() },
        onCreateClick = { isSubmitting = true }
    )

    LaunchedEffect(isSubmitting) {
        if (!isSubmitting) return@LaunchedEffect

        if (selectedType.isBlank()) {
            errorMessage = "Please select a case type."
            isSubmitting = false
            return@LaunchedEffect
        }

        try {
            val created = GetCaseData.createCase(
                petId = petId,
                caseType = selectedType
            )
            if (created?.id != null) {
                navController.navigate(CaseRoutes.caseDetails(created.id)) {
                    popUpTo(CaseRoutes.SearchPetForCase) { inclusive = true }
                }
            } else {
                errorMessage = "Failed to create case. Please try again."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = e.message ?: "An unexpected error occurred."
        } finally {
            isSubmitting = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectCaseTypeContent(
    selectedType: String,
    isSubmitting: Boolean,
    errorMessage: String,
    onTypeSelect: (String) -> Unit,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    val caseTypes = listOf(
        "CONSULTATION", "VACCINATION", "SURGERY",
        "CONFINEMENT", "TREATMENT", "GROOMING", "EMERGENCY"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Case Type") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "What type of case is this?",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }

            caseTypes.forEach { type ->
                val isSelected = selectedType == type
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTypeSelect(type) },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = if (isSelected)
                            androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.primary
                            )
                        else
                            androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.outline
                            )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = type.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = CaseStages.stagesFor(type)
                                    .joinToString(" → ") {
                                        CaseStages.displayName(it)
                                    },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            RadioButton(selected = true, onClick = null)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedType.isNotBlank() && !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating…")
                } else {
                    Text("Create Case")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
