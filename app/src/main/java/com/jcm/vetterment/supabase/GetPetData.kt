package com.jcm.vetterment.supabase


import com.jcm.vetterment.features.data_models.PatientListItem

import com.jcm.vetterment.features.data_models.Pet

import io.github.jan.supabase.auth.auth

import io.github.jan.supabase.postgrest.from


object GetPetData {

    suspend fun createPet(
        pet: Pet
    ): Result<Pet> {

        val normalizedOwnerId =
            pet.owner_id.trim()

        val normalizedName =
            pet.name.trim()

        val normalizedSpecies =
            pet.species.trim().uppercase()

        val normalizedSex =
            pet.sex.trim().uppercase()


        if (normalizedOwnerId.isBlank()) {
            return Result.failure(IllegalArgumentException("Owner ID is blank"))
        }

        if (normalizedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Pet name is blank"))
        }

        if (normalizedSpecies !in listOf("DOG", "CAT")) {
            return Result.failure(IllegalArgumentException("Species must be DOG or CAT"))
        }

        if (normalizedSex !in listOf("MALE", "FEMALE", "UNKNOWN")) {
            return Result.failure(IllegalArgumentException("Sex must be MALE, FEMALE, or UNKNOWN"))
        }


        val owner =
            GetProfileData.getProfileById(
                normalizedOwnerId
            ) ?: return Result.failure(IllegalArgumentException("Owner profile not found"))


        if (owner.role != "pet_owner") {
            return Result.failure(IllegalArgumentException("Owner must have pet_owner role"))
        }


        val currentUser =
            SupabaseClient.client
                .auth
                .currentUserOrNull()
                ?: return Result.failure(IllegalStateException("User not authenticated"))

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
            val createdPet = SupabaseClient.client
                .from("pets")
                .insert(petToInsert) {
                    select()
                }
                .decodeSingle<Pet>()
            Result.success(createdPet)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
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

            pets.mapNotNull { pet -> pet.id?.let { id -> id to pet } }.toMap()
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
    ): Result<Boolean> {

        val petId = pet.id ?: return Result.failure(IllegalArgumentException("Pet ID is null"))

        if (pet.owner_id.isBlank()) return Result.failure(IllegalArgumentException("Owner ID is blank"))

        if (pet.name.isBlank()) return Result.failure(IllegalArgumentException("Pet name is blank"))

        return try {
            val updates = mutableMapOf<String, Any?>(
                "name" to pet.name.trim(),
                "nickname" to pet.nickname?.trim()?.takeIf { it.isNotBlank() },
                "species" to pet.species.trim().uppercase(),
                "breed" to pet.breed?.trim()?.takeIf { it.isNotBlank() },
                "sex" to pet.sex.trim().uppercase(),
                "birth_date" to pet.birth_date?.trim()?.takeIf { it.isNotBlank() },
                "weight" to pet.weight,
                "known_allergies" to pet.known_allergies?.trim()?.takeIf { it.isNotBlank() },
                "pre_existing_conditions" to pet.pre_existing_conditions?.trim()?.takeIf { it.isNotBlank() },
                "photo_url" to pet.photo_url?.trim()?.takeIf { it.isNotBlank() }
            )

            SupabaseClient.client
                .from("pets")
                .update(updates) {
                    filter {
                        eq("id", petId)
                        eq("owner_id", pet.owner_id)
                    }
                }

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            System.err.println("Failed to update pet $petId: ${e.message} | Cause: ${e.cause}")
            Result.failure(e)
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
            System.err.println("Failed to update pet: ${e.message}")
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