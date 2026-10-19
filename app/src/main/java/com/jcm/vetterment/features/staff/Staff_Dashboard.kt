package com.jcm.vetterment.features.staff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.CaseProgress
import com.jcm.vetterment.features.data_models.PatientListItem
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.features.staff.appointments.AppointmentCard
import com.jcm.vetterment.features.staff.cases.CaseStages
import com.jcm.vetterment.navigation.AppointmentRoutes
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.navigation.StaffRoutes
import com.jcm.vetterment.supabase.GetAppointmentData
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetPetData

// ─── Logic layer ──────────────────────────────────────────────────────────────
@Composable
fun StaffDashboardScreen(
    navController: NavController
) {
    var appointments by remember {
        mutableStateOf<List<Appointment>>(emptyList())
    }
    var patientList by remember {
        mutableStateOf<List<PatientListItem>>(emptyList())
    }
    var activeCases by remember {
        mutableStateOf<List<Case>>(emptyList())
    }
    var recentProgress by remember {
        mutableStateOf<List<Pair<CaseProgress, Pet?>>>(emptyList())
    }
    var isLoading by remember {
        mutableStateOf(true)
    }
    var errorMessage by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {

        isLoading = true
        errorMessage = ""

        try {
            // Load appointments
            val loadedAppointments =
                GetAppointmentData.getAllAppointments()
            appointments = loadedAppointments

            // Load patients
            val loadedPatients =
                GetPetData.getPatientList()

            patientList = loadedPatients

            // Load cases
            val loadedCases =
                GetCaseData.getAllCases()

            activeCases = loadedCases.filter {

                it.current_status != "COMPLETED" &&
                        it.current_status != "DISCHARGED"
            }

            // Recent activity
            val progressItems =
                mutableListOf<Pair<CaseProgress, Pet?>>()
            val latestCases =
                loadedCases.take(8)
            latestCases.forEach { currentCase ->

                val progress =
                    GetCaseData.getProgressForCase(
                        currentCase.id!!
                    )
                if (progress.isNotEmpty()) {

                    val latest =
                        progress.maxByOrNull {
                            it.recorded_at ?: ""
                        }
                    if (latest != null) {
                        val pet =
                            GetPetData.getPetById(
                                currentCase.pet_id
                            )
                        progressItems.add(
                            latest to pet
                        )
                    }
                }
            }

            recentProgress =
                progressItems.sortedByDescending {
                    it.first.recorded_at ?: ""
                }.take(5)

        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage =
                "Unable to load dashboard."
        }
        finally {
            isLoading = false
        }
    }

    // Dashboard statistics
    val pendingAppointments =
        appointments.count {
            it.status == "PENDING"
        }
    val checkedInAppointments =
        appointments.count {
            it.status == "CHECKED_IN"
        }
    val confirmedAppointments =
        appointments.count {
            it.status == "CONFIRMED"
        }
    val unclaimedCases =
        activeCases.count {
            it.assigned_vet_id == null
        }
    val activeCaseCount =
        activeCases.size
    val patientCount =
        patientList.size
    val todayAppointments =
        appointments.filter {
            it.status == "PENDING" ||
                    it.status == "CONFIRMED" ||
                    it.status == "CHECKED_IN"
        }

    val needsAttention =
        activeCases.filter {
            it.assigned_vet_id == null ||
                    it.current_status == "WAITING" ||
                    it.current_status == "CHECKED_IN"
        }

    StaffDashboardContent(
        navController = navController,
        isLoading = isLoading,
        errorMessage = errorMessage,
        pendingAppointments = pendingAppointments,
        checkedInAppointments = checkedInAppointments,
        confirmedAppointments = confirmedAppointments,
        activeCases = activeCaseCount,
        unclaimedCases = unclaimedCases,
        totalPatients = patientCount,
        todayAppointments = todayAppointments,
        needsAttentionCases = needsAttention,
        recentProgress = recentProgress
    )
}
// ─── UI layer ─────────────────────────────────────────────────────────────────
@Composable
fun StaffDashboardContent(
    navController: NavController,
    isLoading: Boolean,
    errorMessage: String,
    pendingAppointments: Int,
    checkedInAppointments: Int,
    confirmedAppointments: Int,
    activeCases: Int,
    unclaimedCases: Int,
    totalPatients: Int,
    todayAppointments: List<Appointment>,
    needsAttentionCases: List<Case>,
    recentProgress: List<Pair<CaseProgress, Pet?>>
) {

    StaffScaffold(
        navController = navController
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage.isNotBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),

                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Clinic Overview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Statistics
                    DashboardSection(
                        title = "Overview"
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Patients",
                                value = totalPatients.toString()
                            )
                            DashboardStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Active Cases",
                                value = activeCases.toString()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Pending",
                                value = pendingAppointments.toString()
                            )
                            DashboardStatCard(
                                modifier = Modifier.weight(1f),
                                title = "Checked In",
                                value = checkedInAppointments.toString()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        DashboardStatCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = "Unclaimed Cases",
                            value = unclaimedCases.toString()
                        )
                    }

                    // Quick actions
                    DashboardSection(
                        title = "Quick Actions"
                    ) {
                        QuickActionCard(
                            title = "Patients",
                            subtitle = "View patient records",
                            onClick = {
                                navController.navigate(
                                    StaffRoutes.Patients
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        QuickActionCard(
                            title = "Appointments",
                            subtitle = "Review upcoming appointments",
                            onClick = {
                                navController.navigate(
                                    StaffRoutes.Appointments
                                )
                            }

                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        QuickActionCard(
                            title = "Cases",
                            subtitle = "Open case management",
                            onClick = {
                                navController.navigate(
                                    CaseRoutes.Cases
                                )
                            }
                        )
                    }

                    // Needs Attention
                    DashboardSection(
                        title = "Needs Attention"
                    ) {
                        if (needsAttentionCases.isEmpty()) {
                            Text(
                                text = "No cases require immediate attention.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            needsAttentionCases.take(5).forEach {
                                NeedsAttentionCard(
                                    case = it,
                                    onClick = {
                                        navController.navigate(
                                            CaseRoutes.caseDetails(
                                                it.id!!
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Today's Appointments
                    DashboardSection(
                        title = "Today's Queue"
                    ) {
                        if (todayAppointments.isEmpty()) {
                            Text(
                                text = "No active appointments.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            todayAppointments.take(5).forEach {
                                AppointmentCard(
                                    appointment = it,
                                    onClick = {
                                        navController.navigate(
                                            AppointmentRoutes.appointmentDetail(
                                                it.id!!
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Recent Activity
                    DashboardSection(
                        title = "Recent Activity"
                    ) {
                        if (recentProgress.isEmpty()) {
                            Text(
                                text = "No recent updates.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            recentProgress.forEach {
                                RecentActivityCard(
                                    progress = it.first,
                                    pet = it.second
                                )
                            }
                        }
                    }
                    Spacer(
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}

// Other Functions / Helper Composables
@Composable
private fun DashboardSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        content()
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {

    ElevatedCard(
        modifier = modifier
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NeedsAttentionCard(
    case: Case,
    onClick: () -> Unit
) {

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = case.case_type
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = CaseStages.displayName(case.current_status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AssistChip(
                onClick = {},
                label = {
                    Text(
                        CaseStages.displayName(case.current_status)
                    )
                }
            )
        }
    }
}

@Composable
private fun RecentActivityCard(
    progress: CaseProgress,
    pet: Pet?
) {

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {

            Text(
                text = pet?.name ?: "Unknown Patient",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = CaseStages.displayName(progress.status),
                style = MaterialTheme.typography.bodyMedium
            )

            progress.notes
                ?.takeIf { it.isNotBlank() }
                ?.let {

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            progress.recorded_at
                ?.let {

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
    }
}