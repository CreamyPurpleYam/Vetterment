package com.jcm.vetterment.features.staff.cases

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.components.DatePickerField
import kotlinx.coroutines.launch
import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.CaseProgress
import com.jcm.vetterment.features.data_models.Medication
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetMedicationData
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun CaseDetailsScreen(
    caseId: String,
    navController: NavController
) {
    var case by remember { mutableStateOf<Case?>(null) }
    var pet by remember { mutableStateOf<Pet?>(null) }
    var owner by remember { mutableStateOf<Profile?>(null) }
    var progress by remember { mutableStateOf<List<CaseProgress>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    var isAdvancing by remember { mutableStateOf(false) }
    var isClaiming by remember { mutableStateOf(false) }
    var showAdvanceDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showFollowUpDialog by remember { mutableStateOf(false) }
    var advanceNotes by remember { mutableStateOf("") }
    var currentProfile by remember { mutableStateOf<Profile?>(null) }
    var assignedVet by remember { mutableStateOf<Profile?>(null) }
    var isCreatingFollowUp by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var medications by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var showAddMedicationDialog by remember { mutableStateOf(false) }

    // Initial load — keyed on caseId, not a boolean flag
    LaunchedEffect(caseId) {
        isLoading = true
        errorMessage = ""
        try {
            val fetchedCase = GetCaseData.getCaseById(caseId)
            if (fetchedCase == null) {
                errorMessage = "Case not found."
                return@LaunchedEffect
            }
            case = fetchedCase
            currentProfile = GetProfileData.getCurrentProfile()
            progress = GetCaseData.getProgressForCase(caseId)
            pet = GetPetData.getPetById(fetchedCase.pet_id)
            pet?.let { owner = GetProfileData.getProfileById(it.owner_id) }
            assignedVet = fetchedCase.assigned_vet_id?.let {
                GetProfileData.getProfileById(it)
            }
            medications = GetMedicationData.getMedicationsForCase(caseId)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load case details."
        } finally {
            isLoading = false
        }
    }

    // Advance status
    LaunchedEffect(isAdvancing) {
        if (!isAdvancing) return@LaunchedEffect
        try {
            val updated = GetCaseData.advanceCaseStatus(
                caseId = caseId,
                notes = advanceNotes
            )
            if (updated != null) {
                case = updated
                progress = GetCaseData.getProgressForCase(caseId)
                advanceNotes = ""
            } else {
                errorMessage = "Could not advance status. Make sure the case is claimed."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Failed to update status."
        } finally {
            isAdvancing = false
        }
    }

    // Claim case
    LaunchedEffect(isClaiming) {
        if (!isClaiming) return@LaunchedEffect
        try {
            val updated = GetCaseData.claimCase(caseId)
            if (updated != null) {
                case = updated
                progress = GetCaseData.getProgressForCase(caseId)
                assignedVet = currentProfile
            } else {
                errorMessage = "Unable to claim case."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Failed to claim case."
        } finally {
            isClaiming = false
        }
    }

    // Create follow-up
    LaunchedEffect(isCreatingFollowUp) {
        if (!isCreatingFollowUp) return@LaunchedEffect
        try {
            val currentCase = case ?: return@LaunchedEffect
            val followUp = GetCaseData.createFollowUpCase(currentCase)
            if (followUp?.id != null) {
                navController.navigate(CaseRoutes.caseDetails(followUp.id))
            } else {
                errorMessage = "Failed to create follow-up case."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Failed to create follow-up case."
        } finally {
            isCreatingFollowUp = false
        }
    }

    CaseDetailsContent(
        case = case,
        pet = pet,
        owner = owner,
        assignedVet = assignedVet,
        currentProfile = currentProfile,
        progress = progress,
        isLoading = isLoading,
        errorMessage = errorMessage,
        isClaiming = isClaiming,
        isCreatingFollowUp = isCreatingFollowUp,
        onBackClick = { navController.popBackStack() },
        onAdvanceClick = { showAdvanceDialog = true },
        onClaimClick = { isClaiming = true },
        onEditClick = { showEditDialog = true },
        onFollowUpClick = { showFollowUpDialog = true },medications = medications,
        onAddMedicationClick = { showAddMedicationDialog = true },
    )

    if (showAdvanceDialog) {
        AdvanceStatusDialog(
            currentStatus = case?.current_status ?: "",
            caseType = case?.case_type ?: "",
            notes = advanceNotes,
            onNotesChange = { advanceNotes = it },
            onConfirm = {
                showAdvanceDialog = false
                isAdvancing = true
            },
            onDismiss = { showAdvanceDialog = false }
        )
    }

    if (showEditDialog && case != null) {
        EditCaseDetailsDialog(
            case = case!!,
            onConfirm = { updates ->
                showEditDialog = false
                // Optimistic local update
                case = case!!.copy(
                    diagnosis = updates.diagnosis ?: case!!.diagnosis,
                    treatment_plan = updates.treatmentPlan ?: case!!.treatment_plan,
                    medication_instructions = updates.medicationInstructions
                        ?: case!!.medication_instructions,
                    notes = updates.notes ?: case!!.notes,
                    discharge_summary = updates.dischargeSummary ?: case!!.discharge_summary,
                    admission_date = updates.admissionDate ?: case!!.admission_date,
                    expected_discharge_date = updates.expectedDischargeDate
                        ?: case!!.expected_discharge_date,
                    actual_discharge_date = updates.actualDischargeDate
                        ?: case!!.actual_discharge_date
                )
                // Persist to Supabase in background
                coroutineScope.launch {
                    GetCaseData.updateCaseDetails(
                        caseId = caseId,
                        diagnosis = updates.diagnosis,
                        treatmentPlan = updates.treatmentPlan,
                        medicationInstructions = updates.medicationInstructions,
                        notes = updates.notes,
                        dischargeSummary = updates.dischargeSummary,
                        admissionDate = updates.admissionDate,
                        expectedDischargeDate = updates.expectedDischargeDate,
                        actualDischargeDate = updates.actualDischargeDate
                    )
                }
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showFollowUpDialog) {
        AlertDialog(
            onDismissRequest = { showFollowUpDialog = false },
            title = { Text("Create Follow-up Case") },
            text = {
                Text("This will create a new Consultation case linked to this one. The current case will remain completed.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showFollowUpDialog = false
                    isCreatingFollowUp = true
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showFollowUpDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showAddMedicationDialog) {
        AddMedicationDialog(
            caseId = caseId,
            onDismiss = { showAddMedicationDialog = false },
            onSuccess = {
                showAddMedicationDialog = false
                coroutineScope.launch {
                    medications = GetMedicationData.getMedicationsForCase(caseId)
                }
            }
        )
    }
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailsContent(
    case: Case?,
    pet: Pet?,
    owner: Profile?,
    assignedVet: Profile?,
    currentProfile: Profile?,
    progress: List<CaseProgress>,
    isLoading: Boolean,
    errorMessage: String,
    isClaiming: Boolean,
    isCreatingFollowUp: Boolean,
    onBackClick: () -> Unit,
    onAdvanceClick: () -> Unit,
    onClaimClick: () -> Unit,
    onEditClick: () -> Unit,
    onFollowUpClick: () -> Unit,
    medications: List<Medication>,
    onAddMedicationClick: () -> Unit,
) {
    val isCompleted = case?.current_status in listOf("COMPLETED", "DISCHARGED")
    val isClaimed = case?.assigned_vet_id != null
    val canEdit = isClaimed && !isCompleted

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = case?.case_type?.lowercase()
                                ?.replaceFirstChar { it.uppercase() } ?: "Case Details",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (pet != null) {
                            Text(
                                text = pet.name,
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
                },
                actions = {
                    if (canEdit) {
                        IconButton(onClick = onEditClick) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit clinical details")
                        }
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
                case != null -> {
                    CaseDetailsBody(
                        case = case,
                        pet = pet,
                        owner = owner,
                        assignedVet = assignedVet,
                        currentProfile = currentProfile,
                        progress = progress,
                        isClaiming = isClaiming,
                        isCreatingFollowUp = isCreatingFollowUp,
                        onAdvanceClick = onAdvanceClick,
                        onClaimClick = onClaimClick,
                        onFollowUpClick = onFollowUpClick,
                        medications = medications,
                        onAddMedicationClick = onAddMedicationClick,
                    )
                }
            }
        }
    }
}

// ─── Body ─────────────────────────────────────────────────────────────────────

@Composable
private fun CaseDetailsBody(
    case: Case,
    pet: Pet?,
    owner: Profile?,
    assignedVet: Profile?,
    currentProfile: Profile?,
    progress: List<CaseProgress>,
    isClaiming: Boolean,
    isCreatingFollowUp: Boolean,
    onAdvanceClick: () -> Unit,
    onClaimClick: () -> Unit,
    onFollowUpClick: () -> Unit,
    medications: List<Medication>,
    onAddMedicationClick: () -> Unit,
) {
    val stages = CaseStages.stagesFor(case.case_type)
    val currentIndex = stages.indexOf(case.current_status)
    val isCompleted = case.current_status in listOf("COMPLETED", "DISCHARGED")
    val isClaimed = case.assigned_vet_id != null
    val canClaim =
        (currentProfile?.role == "vet" || currentProfile?.role == "branch_manager") &&
                case.assigned_vet_id == null &&
                !isClaiming &&
                !isCompleted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ── Patient summary ───────────────────────────────────────────────
        if (pet != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (pet.species.uppercase()) {
                                "DOG" -> "🐶"
                                "CAT" -> "🐱"
                                else  -> "🐾"
                            },
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(pet.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = pet.breed ?: pet.species.lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (owner != null) {
                                Text(
                                    text = "Owner: ${owner.full_name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            assignedVet?.let {
                                Text(
                                    text = "Assigned Vet: Dr. ${it.full_name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Claim button ──────────────────────────────────────────────────
        if (canClaim) {
            Button(
                onClick = onClaimClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isClaiming
            ) {
                if (isClaiming) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Claiming…")
                } else {
                    Text("Claim Case")
                }
            }
        }

        // ── Timeline ──────────────────────────────────────────────────────
        HorizontalDivider()
        Text(
            text = "Timeline",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        CaseTimeline(stages = stages, currentIndex = currentIndex)

        // ── Advance / status actions ──────────────────────────────────────
        if (!isCompleted) {
            if (!isClaimed) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This case must be claimed by a veterinarian before it can progress.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val nextStatus = if (currentIndex < stages.lastIndex)
                    CaseStages.displayName(stages[currentIndex + 1]) else null
                if (nextStatus != null) {
                    Button(
                        onClick = onAdvanceClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Advance to $nextStatus")
                    }
                }
            }
        } else {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "This case is ${CaseStages.displayName(case.current_status).lowercase()}.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Follow-up button — only when completed ─────────────────
            Button(
                onClick = onFollowUpClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCreatingFollowUp
            ) {
                if (isCreatingFollowUp) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating…")
                } else {
                    Text("Create Follow-up Case")
                }
            }
        }

        HorizontalDivider()

        // ── Case information ──────────────────────────────────────────────
        Text(
            text = "Case Information",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        CaseDetailRow("Case Type", case.case_type.lowercase()
            .replaceFirstChar { it.uppercase() })
        CaseDetailRow("Status", CaseStages.displayName(case.current_status))
        case.admission_date?.let { CaseDetailRow("Admission Date", it) }
        case.expected_discharge_date?.let { CaseDetailRow("Expected Discharge", it) }
        case.actual_discharge_date?.let { CaseDetailRow("Actual Discharge", it) }

        // ── Clinical details ──────────────────────────────────────────────
        if (listOf(
                case.diagnosis,
                case.treatment_plan,
                case.medication_instructions,
                case.notes,
                case.discharge_summary
            ).any { !it.isNullOrBlank() }
        ) {
            HorizontalDivider()
            Text(
                text = "Clinical Details",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            case.diagnosis?.takeIf { it.isNotBlank() }
                ?.let { CaseDetailRow("Diagnosis", it) }
            case.treatment_plan?.takeIf { it.isNotBlank() }
                ?.let { CaseDetailRow("Treatment Plan", it) }
            case.medication_instructions?.takeIf { it.isNotBlank() }
                ?.let { CaseDetailRow("Medication", it) }
            case.notes?.takeIf { it.isNotBlank() }
                ?.let { CaseDetailRow("Notes", it) }
            case.discharge_summary?.takeIf { it.isNotBlank() }
                ?.let { CaseDetailRow("Discharge Summary", it) }
        }

        // ── Medications ───────────────────────────────────────────────────
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Medications",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (isClaimed && !isCompleted) {
                TextButton(onClick = onAddMedicationClick) { Text("+ Add") }
            }
        }
        if (medications.isEmpty()) {
            Text(
                text = "No medications prescribed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            medications.forEach { med ->
                MedicationCard(medication = med)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // ── Progress history ──────────────────────────────────────────────
        if (progress.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = "Progress History",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            progress.forEach { entry -> ProgressEntry(entry = entry) }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─── Timeline ─────────────────────────────────────────────────────────────────

@Composable
fun CaseTimeline(stages: List<String>, currentIndex: Int) {
    // Horizontal scroll handles long stage lists (CONFINEMENT, GROOMING = 8 stages)
    val nodeSize = 24.dp
    val activeNodeSize = 32.dp
    val minItemWidth = 56.dp

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            stages.forEachIndexed { index, _ ->
                val isCompleted = index < currentIndex
                val isCurrent = index == currentIndex

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .defaultMinSize(minWidth = minItemWidth)
                        .wrapContentWidth()
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = when {
                            isCompleted -> MaterialTheme.colorScheme.primary
                            isCurrent   -> MaterialTheme.colorScheme.primaryContainer
                            else        -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(if (isCurrent) activeNodeSize else nodeSize)
                    ) {}
                    if (isCurrent) {
                        Surface(
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        ) {}
                    }
                }

                if (index < stages.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.width(24.dp),
                        color = if (isCompleted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 2.dp
                    )
                }
            }
        }

        // Labels — same scroll state would be ideal but approximating with same width
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            stages.forEachIndexed { index, stage ->
                val isCompleted = index < currentIndex
                val isCurrent = index == currentIndex
                Text(
                    text = CaseStages.displayName(stage),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .defaultMinSize(minWidth = minItemWidth)
                        .wrapContentWidth()
                        .padding(horizontal = 2.dp),
                    color = when {
                        isCompleted -> MaterialTheme.colorScheme.primary
                        isCurrent   -> MaterialTheme.colorScheme.onSurface
                        else        -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// ─── Edit clinical details dialog ─────────────────────────────────────────────

private data class CaseDetailsEdits(
    val diagnosis: String?,
    val treatmentPlan: String?,
    val medicationInstructions: String?,
    val notes: String?,
    val dischargeSummary: String?,
    val admissionDate: String?,
    val expectedDischargeDate: String?,
    val actualDischargeDate: String?
)

@Composable
private fun EditCaseDetailsDialog(
    case: Case,
    onConfirm: (CaseDetailsEdits) -> Unit,
    onDismiss: () -> Unit
) {
    var diagnosis by remember { mutableStateOf(case.diagnosis ?: "") }
    var treatmentPlan by remember { mutableStateOf(case.treatment_plan ?: "") }
    var medicationInstructions by remember {
        mutableStateOf(case.medication_instructions ?: "")
    }
    var notes by remember { mutableStateOf(case.notes ?: "") }
    var dischargeSummary by remember { mutableStateOf(case.discharge_summary ?: "") }
    var admissionDate by remember { mutableStateOf(case.admission_date ?: "") }
    var expectedDischargeDate by remember {
        mutableStateOf(case.expected_discharge_date ?: "")
    }
    var actualDischargeDate by remember {
        mutableStateOf(case.actual_discharge_date ?: "")
    }
    var isSaving by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Edit Clinical Details") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditField("Diagnosis", diagnosis) { diagnosis = it }
                EditField("Treatment Plan", treatmentPlan, minLines = 2) {
                    treatmentPlan = it
                }
                EditField("Medication Instructions", medicationInstructions, minLines = 2) {
                    medicationInstructions = it
                }
                EditField("Notes", notes, minLines = 2) { notes = it }
                EditField("Discharge Summary", dischargeSummary, minLines = 2) {
                    dischargeSummary = it
                }
                EditField("Admission Date (YYYY-MM-DD)", admissionDate) {
                    admissionDate = it
                }
                EditField("Expected Discharge (YYYY-MM-DD)", expectedDischargeDate) {
                    expectedDischargeDate = it
                }
                EditField("Actual Discharge (YYYY-MM-DD)", actualDischargeDate) {
                    actualDischargeDate = it
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    isSaving = true
                    val edits = CaseDetailsEdits(
                        diagnosis = diagnosis.trim().takeIf { it.isNotBlank() },
                        treatmentPlan = treatmentPlan.trim().takeIf { it.isNotBlank() },
                        medicationInstructions = medicationInstructions.trim()
                            .takeIf { it.isNotBlank() },
                        notes = notes.trim().takeIf { it.isNotBlank() },
                        dischargeSummary = dischargeSummary.trim().takeIf { it.isNotBlank() },
                        admissionDate = admissionDate.trim().takeIf { it.isNotBlank() },
                        expectedDischargeDate = expectedDischargeDate.trim()
                            .takeIf { it.isNotBlank() },
                        actualDischargeDate = actualDischargeDate.trim()
                            .takeIf { it.isNotBlank() }
                    )
                    // Persist to Supabase
                    onConfirm(edits)
                },
                enabled = !isSaving
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        }
    )
}

@Composable
private fun EditField(
    label: String,
    value: String,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = minLines + 2
    )
}

// ─── Advance status dialog ────────────────────────────────────────────────────

@Composable
private fun AdvanceStatusDialog(
    currentStatus: String,
    caseType: String,
    notes: String,
    onNotesChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val stages = CaseStages.stagesFor(caseType)
    val currentIndex = stages.indexOf(currentStatus)
    val nextStatus = if (currentIndex < stages.lastIndex)
        CaseStages.displayName(stages[currentIndex + 1]) else return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advance to $nextStatus?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("You can add optional notes for this status update.")
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun CaseDetailRow(label: String, value: String) {
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

@Composable
private fun ProgressEntry(entry: CaseProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = CaseStages.displayName(entry.status),
            style = MaterialTheme.typography.bodyMedium
        )
        entry.notes?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        entry.recorded_at?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
// ─── Medication card ──────────────────────────────────────────────────────────

@Composable
private fun MedicationCard(medication: Medication) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(medication.name, style = MaterialTheme.typography.titleMedium)
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
            Text(
                text = buildString {
                    append(medication.start_date)
                    if (!medication.end_date.isNullOrBlank()) append(" → ${medication.end_date}")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!medication.special_instructions.isNullOrBlank()) {
                Text(
                    text = medication.special_instructions,
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Add medication dialog ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMedicationDialog(
    caseId: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val frequencyOptions = listOf(
        "Once Daily", "Twice Daily", "Three Times Daily", "Four Times Daily", "As Needed"
    )
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(frequencyOptions.first()) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var specialInstructions by remember { mutableStateOf("") }
    var frequencyExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Add Medication") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medication name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dosage,
                    onValueChange = { dosage = it },
                    label = { Text("Dosage (e.g. 250mg)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = frequencyExpanded,
                    onExpandedChange = { frequencyExpanded = !frequencyExpanded }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(frequencyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = frequencyExpanded,
                        onDismissRequest = { frequencyExpanded = false }
                    ) {
                        frequencyOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { frequency = option; frequencyExpanded = false }
                            )
                        }
                    }
                }
                DatePickerField(
                    value = startDate,
                    label = "Start Date",
                    onValueChange = { startDate = it }
                )

                DatePickerField(
                    value = endDate,
                    label = "End Date (Optional)",
                    onValueChange = { endDate = it }
                )
                OutlinedTextField(
                    value = specialInstructions,
                    onValueChange = { specialInstructions = it },
                    label = { Text("Special instructions (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && name.isNotBlank() && dosage.isNotBlank() && startDate.isNotBlank(),
                onClick = {
                    coroutineScope.launch {
                        isSaving = true
                        errorMessage = null
                        val result = GetMedicationData.addMedication(
                            Medication(
                                case_id = caseId,
                                name = name.trim(),
                                dosage = dosage.trim(),
                                frequency = frequency,
                                start_date = startDate.trim(),
                                end_date = endDate.trim().ifBlank { null },
                                special_instructions = specialInstructions.trim().ifBlank { null }
                            )
                        )
                        isSaving = false
                        if (result != null) onSuccess()
                        else errorMessage = "Failed to save. Please try again."
                    }
                }
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") }
        }
    )
}
