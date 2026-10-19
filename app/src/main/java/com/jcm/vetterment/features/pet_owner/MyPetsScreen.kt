package com.jcm.vetterment.features.pet_owner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Pet
import com.jcm.vetterment.navigation.PetOwnerRoutes
import com.jcm.vetterment.supabase.GetPetData
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun MyPetsScreen(navController: NavController) {

    var pets by remember { mutableStateOf<List<Pet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = ""
        try {
            val profile = GetProfileData.getCurrentProfile() ?: run {
                errorMessage = "Unable to load profile."
                return@LaunchedEffect
            }
            pets = GetPetData.getPetsForOwner(profile.id)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load pets."
        } finally {
            isLoading = false
        }
    }

    MyPetsContent(
        navController = navController,
        pets = pets,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onPetClick = { navController.navigate(PetOwnerRoutes.petDetail(it.id!!)) }
    )
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@Composable
fun MyPetsContent(
    navController: NavController,
    pets: List<Pet>,
    isLoading: Boolean,
    errorMessage: String,
    onPetClick: (Pet) -> Unit
) {
    PetOwnerScaffold(navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text("My Pets", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage.isNotBlank()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@PetOwnerScaffold
            }

            if (pets.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No pets registered yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@PetOwnerScaffold
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(pets) { pet ->
                    PetCard(pet = pet, onClick = { onPetClick(pet) })
                }
            }
        }
    }
}

// ─── Pet detail screen ────────────────────────────────────────────────────────

@Composable
fun PetOwnerPetDetailScreen(petId: String, navController: NavController) {

    var pet by remember { mutableStateOf<Pet?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(petId) {
        isLoading = true
        errorMessage = ""
        try {
            pet = GetPetData.getPetById(petId) ?: run {
                errorMessage = "Pet not found."
                return@LaunchedEffect
            }
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to load pet."
        } finally {
            isLoading = false
        }
    }

    PetOwnerPetDetailContent(
        pet = pet,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBackClick = { navController.popBackStack() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetOwnerPetDetailContent(
    pet: Pet?,
    isLoading: Boolean,
    errorMessage: String,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(pet?.name ?: "Pet Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                errorMessage.isNotBlank() -> Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
                pet != null -> PetOwnerPetDetailBody(pet = pet)
            }
        }
    }
}

@Composable
private fun PetOwnerPetDetailBody(pet: Pet) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Identity ──────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (pet.species.uppercase()) {
                            "DOG" -> "🐶"
                            "CAT" -> "🐱"
                            else  -> "🐾"
                        },
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(pet.name, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = pet.species.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ── Profile details ───────────────────────────────────────────────
        HorizontalDivider()
        Text(
            "Profile",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        pet.breed?.let { PetDetailRow("Breed", it) }
        PetDetailRow("Sex", pet.sex.lowercase().replaceFirstChar { it.uppercase() })
        pet.birth_date?.let { PetDetailRow("Birth Date", it) }
        pet.weight?.let { PetDetailRow("Weight", "${it} kg") }

        // ── Medical info ──────────────────────────────────────────────────
        if (!pet.known_allergies.isNullOrBlank() || !pet.pre_existing_conditions.isNullOrBlank()) {
            HorizontalDivider()
            Text(
                "Medical Information",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            pet.known_allergies?.takeIf { it.isNotBlank() }?.let {
                PetDetailRow("Known Allergies", it)
            }
            pet.pre_existing_conditions?.takeIf { it.isNotBlank() }?.let {
                PetDetailRow("Pre-existing Conditions", it)
            }
        }

        // ── Change request notice ─────────────────────────────────────────
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "If any of this information is incorrect, please contact the clinic to request a correction.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─── Shared helper composables ────────────────────────────────────────────────

@Composable
fun PetCard(pet: Pet, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (pet.species.uppercase()) {
                    "DOG" -> "🐶"
                    "CAT" -> "🐱"
                    else  -> "🐾"
                },
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(pet.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = buildString {
                        append(pet.species.lowercase().replaceFirstChar { it.uppercase() })
                        pet.breed?.let { append(" • $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PetDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
}
