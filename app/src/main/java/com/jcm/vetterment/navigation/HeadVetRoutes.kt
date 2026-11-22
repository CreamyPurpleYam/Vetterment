package com.jcm.vetterment.navigation

object HeadVetRoutes {
    const val Dashboard = "head_vet_dashboard"
    const val StaffList = "head_vet_staff_list"
    const val CreateStaffAccount = "head_vet_create_staff"
    const val Patients = "head_vet_patients"
    const val Appointments = "head_vet_appointments"
    const val Schedules = "head_vet_schedules"
    const val Profile = "head_vet_profile"
}

// Deprecated alias for backwards compatibility
@Deprecated("Use HeadVetRoutes instead", ReplaceWith("HeadVetRoutes"))
typealias BranchManagerRoutes = HeadVetRoutes
