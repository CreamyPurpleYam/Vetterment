package com.jcm.vetterment.navigation

object CaseRoutes {
    const val Cases = "cases"

    const val SearchPetForCase = "search_pet_for_case"

    const val SelectCaseType = "select_case_type/{petId}"
    fun selectCaseType(petId: String) = "select_case_type/$petId"

    const val CaseDetails = "case_details/{caseId}"
    fun caseDetails(caseId: String) = "case_details/$caseId"
}