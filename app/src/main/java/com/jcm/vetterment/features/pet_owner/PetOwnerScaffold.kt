package com.jcm.vetterment.features.pet_owner

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.jcm.vetterment.navigation.PetOwnerRoutes

// ─── Nav item model ───────────────────────────────────────────────────────────

private data class PetOwnerNavItem(
    val label: String,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val petOwnerNavItems = listOf(
    PetOwnerNavItem(
        label = "Dashboard",
        route = PetOwnerRoutes.Dashboard,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    PetOwnerNavItem(
        label = "Appointments",
        route = PetOwnerRoutes.Appointments,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ),
    PetOwnerNavItem(
        label = "My Pets",
        route = PetOwnerRoutes.MyPets,
        selectedIcon = Icons.Filled.Pets,
        unselectedIcon = Icons.Outlined.Pets
    ),
    PetOwnerNavItem(
        label = "Cases",
        route = PetOwnerRoutes.Cases,
        selectedIcon = Icons.Filled.MedicalServices,
        unselectedIcon = Icons.Outlined.MedicalServices
    ),
    PetOwnerNavItem(
        label = "Medicines",
        route = PetOwnerRoutes.Medications,
        // Use extended icons if available; otherwise fall back to LocalPharmacy
        selectedIcon = Icons.Filled.LocalPharmacy,   // or Icons.Filled.Medication
        unselectedIcon = Icons.Outlined.LocalPharmacy // or Icons.Outlined.Medication
    ),
    PetOwnerNavItem(
        label = "Profile",
        route = PetOwnerRoutes.Profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)

// ─── Scaffold ─────────────────────────────────────────────────────────────────

/**
 * Shared scaffold for all pet-owner-side screens.
 *
 * Usage:
 *   PetOwnerScaffold(navController = navController) { innerPadding ->
 *       YourScreenContent(modifier = Modifier.padding(innerPadding))
 *   }
 *
 * Pass [floatingActionButton] to show a FAB on screens that need one.
 */
@Composable
fun PetOwnerScaffold(
    navController: NavController,
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                petOwnerNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(item.route) {
                                    popUpTo(PetOwnerRoutes.Dashboard) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon
                                else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        floatingActionButton = floatingActionButton
    ) { innerPadding ->
        content(innerPadding)
    }
}
