package com.jcm.vetterment.navigation

object PetOwnerRoutes {
    // Bottom-nav destinations.
    // "Dashboard" reuses the existing "pet_owner" route so the post-login
    // role redirect (pet_owner -> PetOwner_Dashboard) keeps working as-is.
    const val Dashboard = "pet_owner"

    const val Appointments = "pet_owner_appointments"
    const val MyPets = "pet_owner_pets"
    const val Cases = "pet_owner_cases"
    const val Profile = "pet_owner_profile"
    const val CaseDetail = "pet_owner_case_detail/{caseId}"
    const val Medications = "pet_owner_medications"
    fun caseDetail(caseId: String) = "pet_owner_case_detail/$caseId"
    const val AppointmentDetail = "pet_owner_appointment_detail/{appointmentId}"
    fun appointmentDetail(appointmentId: String) = "pet_owner_appointment_detail/$appointmentId"
    const val BookAppointment = "pet_owner_book_appointment"
    const val PetDetail = "pet_owner_pet_detail/{petId}"
    fun petDetail(petId: String) = "pet_owner_pet_detail/$petId"
    const val PaymentProcessing = "pet_owner_payment_processing/{orderId}/{isCancel}"
    fun paymentProcessing(orderId: String, isCancel: Boolean) =
        "pet_owner_payment_processing/$orderId/$isCancel"
}
