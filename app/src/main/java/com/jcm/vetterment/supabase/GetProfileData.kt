package com.jcm.vetterment.supabase

import com.jcm.vetterment.features.data_models.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc

object GetProfileData {
    suspend fun getCurrentProfile(): Profile? {
        val user = SupabaseClient.client.auth.currentUserOrNull() ?: return null
        return try {
            SupabaseClient.client
                .from("profiles")
                .select { filter { eq("id", user.id) } }
                .decodeSingle<Profile>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getCurrentRole(): String? {
        return getCurrentProfile()?.role
    }

    suspend fun searchOwners(searchText: String): List<Profile> {
        val normalizedSearch = searchText.trim()
        if (normalizedSearch.isBlank()) return emptyList()
        return try {
            SupabaseClient.client
                .from("profiles")
                .select {
                    filter {
                        eq("role", "pet_owner")
                        or {
                            ilike("full_name", "%$normalizedSearch%")
                            ilike("email", "%$normalizedSearch%")
                        }
                    }
                    range(from = 0, to = 19)
                }
                .decodeList<Profile>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getProfileById(profileId: String): Profile? {
        if (profileId.isBlank()) return null
        return try {
            SupabaseClient.client
                .from("profiles")
                .select { filter { eq("id", profileId) } }
                .decodeSingle<Profile>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getProfilesByIds(profileIds: List<String>): Map<String, Profile> {
        if (profileIds.isEmpty()) return emptyMap()
        return try {
            val profiles = SupabaseClient.client
                .from("profiles")
                .select { filter { isIn("id", profileIds) } }
                .decodeList<Profile>()
            profiles.mapNotNull { p -> p.id?.let { id -> id to p } }.toMap()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    // ─── Staff management (branch manager only) ───────────────────────────────

    suspend fun getAllStaff(): List<Profile> {
        return try {
            SupabaseClient.client
                .from("profiles")
                .select {
                    filter {
                        isIn(
                            "role",
                            listOf("vet", "nurse", "assistant_vet", "branch_manager")
                        )
                    }
                }
                .decodeList<Profile>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Creates a staff account without affecting the current session.
     * Calls the `create_staff_account` Supabase DB function (SECURITY DEFINER).
     * Returns the new profile's UUID on success, null on failure.
     */
    suspend fun createStaffAccount(
        fullName: String,
        email: String,
        password: String,
        role: String
    ): String? {
        val normalizedRole = role.trim().lowercase()
        if (normalizedRole !in listOf("vet", "nurse", "assistant_vet")) return null

        val normalizedEmail = email.trim().lowercase()
        val normalizedName = fullName.trim()

        if (normalizedName.isBlank()) return null
        if (normalizedEmail.isBlank()) return null
        if (password.length < 8) return null

        return try {
            val result = SupabaseClient.client
                .postgrest
                .rpc(
                    "create_staff_account",
                    mapOf(
                        "p_email" to normalizedEmail,
                        "p_password" to password,
                        "p_full_name" to normalizedName,
                        "p_role" to normalizedRole
                    )
                )
            result.decodeAs<String>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ─── Staff management - Update/Delete (Branch Manager only) ──────────────────

    /**
     * Updates a staff member's profile information.
     * Only allows updating full_name and role for security.
     */
    suspend fun updateStaffProfile(
        profileId: String,
        fullName: String? = null,
        role: String? = null
    ): Result<Boolean> {
        if (profileId.isBlank()) return Result.failure(IllegalArgumentException("Profile ID is blank"))

        val updates = mutableMapOf<String, Any?>()
        fullName?.let { updates["full_name"] = it.trim().takeIf { it.isNotBlank() } }
        role?.let {
            val normalizedRole = it.trim().lowercase()
            if (normalizedRole in listOf("vet", "nurse", "assistant_vet", "branch_manager")) {
                updates["role"] = normalizedRole
            }
        }

        if (updates.isEmpty()) return Result.failure(IllegalArgumentException("No valid updates provided"))

        return try {
            SupabaseClient.client
                .from("profiles")
                .update(updates) {
                    filter { eq("id", profileId) }
                }
            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Deletes a staff member's profile.
     * This should be used with caution - consider deactivating instead.
     */
    suspend fun deleteStaffProfile(profileId: String): Boolean {
        if (profileId.isBlank()) return false

        return try {
            SupabaseClient.client
                .from("profiles")
                .delete {
                    filter { eq("id", profileId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
