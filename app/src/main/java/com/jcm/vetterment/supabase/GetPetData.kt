package com.jcm.vetterment.supabase

import com.jcm.vetterment.features.data_models.PatientListItem
import com.jcm.vetterment.features.data_models.Pet
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

object GetPetData {

    suspend fun createPet(
        pet: Pet
    ): Pet? {

        val normalizedOwnerId =
            pet.owner_id.trim()
        val normalizedName =
            pet.name.trim()
        val normalizedSpecies =
            pet.species.trim().uppercase()
        val normalizedSex =
            pet.sex.trim().uppercase()

        if (normalizedOwnerId.isBlank()) {
            return null
        }
        if (normalizedName.isBlank()) {
            return null
        }
        if (normalizedSpecies !in listOf("DOG", "CAT")) {
            return null
        }
        if (normalizedSex !in listOf("MALE", "FEMALE", "UNKNOWN")) {
            return null
        }

        val owner =
            GetProfileData.getProfileById(
                normalizedOwnerId
            ) ?: return null

        if (owner.role != "pet_owner") {
            return null
        }

        val currentUser =
            SupabaseClient.client
                .auth
                .currentUserOrNull()
                ?: return null
        val petToInsert = pet.copy(

            owner_id = normalizedOwnerId,

            created_by = currentUser.id,

            name = normalizedName,
            nickname = pet.nickname
                ?.trim()
                ?.takeIf { it.isNotBlank() },
            species = normalizedSpecies,

            breed = pet.breed
                ?.trim()
                ?.takeIf { it.isNotBlank() },

            sex = normalizedSex,

            birth_date = pet.birth_date
                ?.trim()
                ?.takeIf { it.isNotBlank() },

            weight = pet.weight,

            known_allergies = pet.known_allergies
                ?.trim()
                ?.takeIf { it.isNotBlank() },

            pre_existing_conditions = pet.pre_existing_conditions
                ?.trim()
                ?.takeIf { it.isNotBlank() },

            photo_url = pet.photo_url
                ?.trim()
                ?.takeIf { it.isNotBlank() },

            is_active = true
        )

        return try {

            SupabaseClient.client
                .from("pets")
                .insert(petToInsert) {
                    select()
                }
                .decodeSingle<Pet>()

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }

    suspend fun getPetById(
        petId: String
    ): Pet? {

        if (petId.isBlank()) {
            return null
        }

        return try {

            SupabaseClient.client
                .from("pets")
                .select {
                    filter {
                        eq("id", petId)
                        eq("is_active", true)
                    }
                }
                .decodeSingle<Pet>()

        } catch (e: Exception) {

            e.printStackTrace()
            null
        }
    }

    suspend fun getPetsByIds(
        petIds: List<String>
    ): Map<String, Pet> {

        if (petIds.isEmpty()) {
            return emptyMap()
        }

        return try {

            val pets = SupabaseClient.client
                .from("pets")
                .select {
                    filter {
                        isIn("id", petIds)
                        eq("is_active", true)
                    }
                }
                .decodeList<Pet>()

            pets.associateBy { it.id!! }

        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    suspend fun getPetsForOwner(
        ownerId: String
    ): List<Pet> {

        if (ownerId.isBlank()) {
            return emptyList()
        }

        return try {

            SupabaseClient.client
                .from("pets")
                .select {
                    filter {
                        eq("owner_id", ownerId)
                        eq("is_active", true)
                    }
                }
                .decodeList<Pet>()

        } catch (e: Exception) {

            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun updatePet(
        pet: Pet
    ): Boolean {

        val petId = pet.id ?: return false

        if (pet.owner_id.isBlank()) return false

        if (pet.name.isBlank()) return false

        if (pet.species.uppercase() !in listOf("DOG", "CAT")) {
            return false
        }

        if (pet.sex.uppercase() !in listOf("MALE", "FEMALE", "UNKNOWN")) {
            return false
        }

        return try {

            val updatedPet = pet.copy(

                name = pet.name.trim(),
                nickname = pet.nickname
                    ?.trim()
                    ?.takeIf { it.isNotBlank() },
                species = pet.species.trim().uppercase(),
                breed = pet.breed?.trim(),
                sex = pet.sex.trim().uppercase(),
                known_allergies =
                    pet.known_allergies?.trim(),
                pre_existing_conditions =
                    pet.pre_existing_conditions?.trim(),
                photo_url =
                    pet.photo_url?.trim()
            )

            SupabaseClient.client
                .from("pets")
                .update(updatedPet) {
                    filter {
                        eq("id", petId)
                    }
                }

            true

        } catch (e: Exception) {

            e.printStackTrace()
            false
        }
    }

    suspend fun deactivatePet(
        petId: String
    ): Boolean {

        if (petId.isBlank()) {
            return false
        }
        return try {
            SupabaseClient.client
                .from("pets")
                .update(
                    mapOf(
                        "is_active" to false
                    )
                ) {
                    filter {
                        eq("id", petId)
                    }
                }

            true

        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getPatientList(
        searchText: String = ""
    ): List<PatientListItem> {

        val normalizedSearch =
            searchText.trim()

        return try {

            if (normalizedSearch.isBlank()) {
                SupabaseClient.client
                    .from("patient_list")
                    .select {
                        limit(20)
                    }
                    .decodeList<PatientListItem>()

            } else {
                SupabaseClient.client
                    .from("patient_list")
                    .select {
                        filter {
                            or {
                                ilike(
                                    "pet_name",
                                    "%$normalizedSearch%"
                                )
                                ilike(
                                    "owner_name",
                                    "%$normalizedSearch%"
                                )
                            }
                        }
                        limit(20)
                    }
                    .decodeList<PatientListItem>()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}