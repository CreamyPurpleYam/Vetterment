package com.jcm.vetterment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jcm.vetterment.features.auth.LoginScreen
import com.jcm.vetterment.features.auth.RegisterScreen
import com.jcm.vetterment.features.branchmanager.BranchManagerDashboardScreen
import com.jcm.vetterment.features.branchmanager.CreateStaffScreen
import com.jcm.vetterment.features.branchmanager.StaffListScreen
import com.jcm.vetterment.features.pet_owner.BookAppointmentScreen
import com.jcm.vetterment.features.pet_owner.MyPetsScreen
import com.jcm.vetterment.features.pet_owner.PetOwner_Dashboard
import com.jcm.vetterment.features.pet_owner.PetOwnerAppointmentDetailScreen
import com.jcm.vetterment.features.pet_owner.PetOwnerAppointmentsScreen
import com.jcm.vetterment.features.pet_owner.PetOwnerCaseDetailScreen
import com.jcm.vetterment.features.pet_owner.PetOwnerCasesScreen
import com.jcm.vetterment.features.pet_owner.PetOwnerMedicationsScreen
import com.jcm.vetterment.features.pet_owner.PetOwnerPetDetailScreen
import com.jcm.vetterment.features.pet_owner.PaymentProcessingScreen
import com.jcm.vetterment.features.pet_owner.PetOwnerProfileScreen
import com.jcm.vetterment.features.staff.StaffDashboardScreen
import com.jcm.vetterment.features.staff.StaffProfileScreen
import com.jcm.vetterment.features.staff.appointments.AppointmentDetailScreen
import com.jcm.vetterment.features.staff.appointments.AppointmentsScreen
import com.jcm.vetterment.features.staff.cases.CaseDetailsScreen
import com.jcm.vetterment.features.staff.cases.CasesScreen
import com.jcm.vetterment.features.staff.cases.SearchPetForCaseScreen
import com.jcm.vetterment.features.staff.cases.SelectCaseTypeScreen
import com.jcm.vetterment.features.staff.patients.PatientsScreen
import com.jcm.vetterment.features.staff.patients.PetDetailsScreen
import com.jcm.vetterment.features.staff.patients.RegisterPetScreen
import com.jcm.vetterment.features.staff.patients.SearchOwnerScreen
import com.jcm.vetterment.navigation.PetOwnerRoutes
import com.jcm.vetterment.navigation.BranchManagerRoutes
import com.jcm.vetterment.navigation.StaffRoutes
import com.jcm.vetterment.navigation.CaseRoutes
import com.jcm.vetterment.navigation.AppointmentRoutes
import com.jcm.vetterment.supabase.GetProfileData
import com.jcm.vetterment.supabase.SupabaseClient
import com.jcm.vetterment.ui.theme.VettermentTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VettermentTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val authChecked = remember { mutableStateOf(false) }
    val initialDestination = remember { mutableStateOf("login") }

    // Check for existing session on app start
    LaunchedEffect(Unit) {
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        if (currentUser != null) {
            val role = GetProfileData.getCurrentRole()
            initialDestination.value = when(role) {
                "pet_owner" -> "pet_owner"
                "vet", "assistant_vet", "nurse" -> StaffRoutes.Dashboard
                "branch_manager" -> "branch_manager"
                else -> "login"
            }
        }
        authChecked.value = true
    }

    if (!authChecked.value) {
        // Show loading while checking auth state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        NavHost(navController, startDestination = initialDestination.value) {
            // ── Auth ──────────────────────────────────────────────────────────────
            composable("login") {
                LoginScreen(navController)
            }
            composable("register") {
                RegisterScreen(navController)
            }

            // ── Pet Owner ─────────────────────────────────────────────────────────
            composable("pet_owner") {
                PetOwner_Dashboard(navController)
            }
            composable(PetOwnerRoutes.Appointments) {
                PetOwnerAppointmentsScreen(navController)
            }
            composable(PetOwnerRoutes.MyPets) {
                MyPetsScreen(navController)
            }
            composable(PetOwnerRoutes.Cases) {
                PetOwnerCasesScreen(navController)
            }
            composable(PetOwnerRoutes.Profile) {
                PetOwnerProfileScreen(navController)
            }
            composable(PetOwnerRoutes.Medications) {
                PetOwnerMedicationsScreen(navController)
            }
            composable(PetOwnerRoutes.BookAppointment) {
                BookAppointmentScreen(navController)
            }
            composable(PetOwnerRoutes.PaymentProcessing) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getString("orderId")
                    ?: return@composable
                val isCancel = backStackEntry.arguments?.getString("isCancel") == "true"
                PaymentProcessingScreen(
                    orderId = orderId,
                    isCancel = isCancel,
                    navController = navController
                )
            }
            composable(PetOwnerRoutes.CaseDetail) { backStackEntry ->
                val caseId = backStackEntry.arguments?.getString("caseId")
                    ?: return@composable
                PetOwnerCaseDetailScreen(caseId = caseId, navController = navController)
            }
            composable(PetOwnerRoutes.AppointmentDetail) { backStackEntry ->
                val appointmentId = backStackEntry.arguments?.getString("appointmentId")
                    ?: return@composable
                PetOwnerAppointmentDetailScreen(
                    appointmentId = appointmentId,
                    navController = navController
                )
            }
            composable(PetOwnerRoutes.PetDetail) { backStackEntry ->
                val petId = backStackEntry.arguments?.getString("petId")
                    ?: return@composable
                PetOwnerPetDetailScreen(petId = petId, navController = navController)
            }

            // ── Branch Manager ────────────────────────────────────────────────────
            composable("branch_manager") {
                BranchManagerDashboardScreen(navController)
            }
            composable(BranchManagerRoutes.Dashboard) {
                BranchManagerDashboardScreen(navController)
            }
            composable(BranchManagerRoutes.StaffList) {
                StaffListScreen(navController)
            }
            composable(BranchManagerRoutes.CreateStaffAccount) {
                CreateStaffScreen(navController)
            }

            // ── Staff — Dashboard ─────────────────────────────────────────────────
            composable(StaffRoutes.Dashboard) {
                StaffDashboardScreen(navController)
            }

            // ── Staff — Patients ──────────────────────────────────────────────────
            composable(StaffRoutes.Patients) {
                PatientsScreen(navController)
            }
            composable(StaffRoutes.SearchOwner) {
                SearchOwnerScreen(navController)
            }
            composable(StaffRoutes.RegisterPet) { backStackEntry ->
                val ownerId = backStackEntry.arguments?.getString("ownerId")
                    ?: return@composable
                RegisterPetScreen(ownerId = ownerId, navController = navController)
            }
            composable(StaffRoutes.PetDetails) { backStackEntry ->
                val petId = backStackEntry.arguments?.getString("petId")
                    ?: return@composable
                PetDetailsScreen(petId = petId, navController = navController)
            }

            // ── Staff — Cases ─────────────────────────────────────────────────────
            composable(CaseRoutes.Cases) {
                CasesScreen(navController)
            }
            composable(CaseRoutes.SearchPetForCase) {
                SearchPetForCaseScreen(navController)
            }
            composable(CaseRoutes.SelectCaseType) { backStackEntry ->
                val petId = backStackEntry.arguments?.getString("petId")
                    ?: return@composable
                SelectCaseTypeScreen(petId = petId, navController = navController)
            }
            composable(CaseRoutes.CaseDetails) { backStackEntry ->
                val caseId = backStackEntry.arguments?.getString("caseId")
                    ?: return@composable
                CaseDetailsScreen(caseId = caseId, navController = navController)
            }

            // ── Staff — Appointments ──────────────────────────────────────────────
            composable(AppointmentRoutes.Appointments) {
                AppointmentsScreen(navController)
            }
            composable(AppointmentRoutes.AppointmentDetail) { backStackEntry ->
                val appointmentId = backStackEntry.arguments?.getString("appointmentId")
                    ?: return@composable
                AppointmentDetailScreen(
                    appointmentId = appointmentId,
                    navController = navController
                )
            }

            // ── Staff — Profile ───────────────────────────────────────────────────
            composable(StaffRoutes.Profile) {
                StaffProfileScreen(
                    navController = navController,
                    rootNavController = navController
                )
            }
        }
    }
}