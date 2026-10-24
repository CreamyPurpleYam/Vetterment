package com.jcm.vetterment.navigation

object BranchManagerRoutes {
    const val Dashboard = "branch_manager_dashboard"

    const val StaffList = "branch_manager_staff_list"

    const val CreateStaffAccount = "branch_manager_create_staff"

    const val Patients = "branch_manager_patients"
    const val Appointments = "branch_manager_appointments"
    const val Profile = "branch_manager_profile"

    const val StaffDetail = "branch_manager_staff_detail/{profileId}"
    fun staffDetail(profileId: String) = "branch_manager_staff_detail/$profileId"
}
