package com.jcm.vetterment.navigation

object AppointmentRoutes {
    const val Appointments = "appointments"

    const val AppointmentDetail = "appointment_detail/{appointmentId}"
    fun appointmentDetail(appointmentId: String) = "appointment_detail/$appointmentId"

    const val CreateAppointment = "create_appointment"
}
