package com.jcm.vetterment.navigation

object StaffRoutes {

    const val Dashboard  = "staff_dashboard"
    const val Patients   = "patients"
    const val PetDetails = "pet_details/{petId}"
    fun petDetails(petId: String) = "pet_details/$petId"
    const val SearchOwner = "search_owner"
    const val RegisterPet = "register_pet/{ownerId}"
    fun registerPet(ownerId: String) = "register_pet/$ownerId"
    const val Appointments  = "appointments"
    const val Profile    = "staff_profile"
}
