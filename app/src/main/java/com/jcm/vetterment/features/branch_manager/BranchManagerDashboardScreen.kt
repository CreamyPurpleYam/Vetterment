package com.jcm.vetterment.features.branchmanager

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Appointment
import com.jcm.vetterment.features.data_models.Case
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.features.staff.cases.CaseStages
import com.jcm.vetterment.navigation.AppointmentRoutes
import com.jcm.vetterment.navigation.BranchManagerRoutes
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.navigation.StaffRoutes
import com.jcm.vetterment.supabase.GetAppointmentData
import com.jcm.vetterment.supabase.GetCaseData
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun BranchManagerDashboardScreen(navController: NavController) {
    var profile by remember { mutableStateOf<Profile?>(null) }
    var activeCases by remember { mutableStateOf<List<Case>>(emptyList()) }
    var unassignedCases by remember { mutableStateOf<List<Case>>(emptyList()) }
    var todayAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var pendingAppointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var totalStaff by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = ""
        try {
            profile = GetProfileData.getCurrentProfile()

            val allCases = GetCaseData.getAllCases()
            activeCases = allCases.filter {
                it.current_status !in listOf("COMPLETED", "DISCHARGED")
            }
            unassignedCases = activeCases.filter { it.assigned_vet_id == null }

            val allAppointments = GetAppointmentData.getAllAppointments()
            pendingAppointments = allAppointments.filter { it.status == "PENDING" }
            todayAppointments = allAppointments.filter {
                it.status in listOf("CONFIRMED", "CHECKED_IN")
            }

            totalStaff = GetProfileData.getAllStaff().count {
                it.role in listOf("vet", "assistant_vet", "nurse")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load dashboard."
        } finally {
            isLoading = false
        }
    }

    BranchManagerDashboardContent(
        navController = navController,
        profile = profile,
        activeCaseCount = activeCases.size,
        unassignedCaseCount = unassignedCases.size,
        todayAppointmentCount = todayAppointments.size,
        pendingAppointmentCount = pendingAppointments.size,
        totalStaff = totalStaff,
        recentCases = activeCases.take(3),
        isLoading = isLoading,
        errorMessage = errorMessage,
        onViewCasesClick = { navController.navigate(CaseRoutes.Cases) },
        onViewAppointmentsClick = { navController.navigate(AppointmentRoutes.Appointments) },
        onViewStaffClick = { navController.navigate(BranchManagerRoutes.StaffList) },
        onCreateStaffClick = { navController.navigate(BranchManagerRoutes.CreateStaffAccount) },
        onCaseClick = { it.id?.let { id -> navController.navigate(CaseRoutes.caseDetails(id)) } },
        onViewPatientsClick = { navController.navigate(StaffRoutes.Patients) }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun BranchManagerDashboardContent(
    navController: NavController,
    profile: Profile?,
    activeCaseCount: Int,
    unassignedCaseCount: Int,
    todayAppointmentCount: Int,
    pendingAppointmentCount: Int,
    totalStaff: Int,
    recentCases: List<Case>,
    isLoading: Boolean,
    errorMessage: String,
    onViewCasesClick: () -> Unit,
    onViewAppointmentsClick: () -> Unit,
    onViewStaffClick: () -> Unit,
    onCreateStaffClick: () -> Unit,
    onCaseClick: (Case) -> Unit,
    onViewPatientsClick: () -> Unit
) {
    BranchManagerScaffold(navController = navController) { innerPadding ->

        if (isLoading) {
            Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@BranchManagerScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Greeting ──────────────────────────────────────────────────
            profile?.let {
                Text(
                    text = "Welcome, ${it.full_name.split(" ").first()}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Branch Manager",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            }

            // ── Overview cards ────────────────────────────────────────────
            Text("Overview", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverviewCard(
                    label = "Active Cases",
                    count = activeCaseCount,
                    icon = Icons.Default.MedicalServices,
                    modifier = Modifier.weight(1f),
                    onClick = onViewCasesClick
                )
                OverviewCard(
                    label = "Unassigned",
                    count = unassignedCaseCount,
                    icon = Icons.Default.MedicalServices,
                    modifier = Modifier.weight(1f),
                    onClick = onViewCasesClick,
                    isAlert = unassignedCaseCount > 0
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OverviewCard(
                    label = "Today",
                    count = todayAppointmentCount,
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f),
                    onClick = onViewAppointmentsClick
                )
                OverviewCard(
                    label = "Pending Appts",
                    count = pendingAppointmentCount,
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f),
                    onClick = onViewAppointmentsClick,
                    isAlert = pendingAppointmentCount > 0
                )
            }

            // ── Quick actions ─────────────────────────────────────────────
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    label = "Add Staff",
                    icon = Icons.Default.Add,
                    modifier = Modifier.weight(1f),
                    onClick = onCreateStaffClick
                )
                QuickActionButton(
                    label = "View Staff",
                    icon = Icons.Default.Group,
                    modifier = Modifier.weight(1f),
                    onClick = onViewStaffClick
                )
                QuickActionButton(
                    label = "Patients",
                    icon = Icons.Default.MedicalServices,
                    modifier = Modifier.weight(1f),
                    onClick = onViewPatientsClick
                )
            }

            // ── Recent active cases ───────────────────────────────────────
            if (recentCases.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Active Cases", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onViewCasesClick() }
                    )
                }
                recentCases.forEach { case ->
                    DashboardCaseRow(case = case, onClick = { onCaseClick(case) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun OverviewCard(
    label: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isAlert: Boolean = false
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert && count > 0)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAlert && count > 0)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.headlineSmall,
                color = if (isAlert && count > 0)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isAlert && count > 0)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DashboardCaseRow(case: Case, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = case.case_type.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = if (case.assigned_vet_id != null) "Assigned" else "Unassigned",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (case.assigned_vet_id == null)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = CaseStages.displayName(case.current_status),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (case.assigned_vet_id == null)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
