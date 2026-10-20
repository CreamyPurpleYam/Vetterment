package com.jcm.vetterment.features.staff.patients

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.data_models.Profile
import com.jcm.vetterment.navigation.StaffRoutes
import com.jcm.vetterment.supabase.GetProfileData
import kotlinx.coroutines.delay

@Composable
fun SearchOwnerScreen(
    navController: NavController
) {
    var searchText by remember { mutableStateOf("") }
    var owners by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    SearchOwnerContent(
        searchText = searchText,
        owners = owners,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onSearchChange = { searchText = it },
        onOwnerClick = { selectedOwner ->
            navController.navigate(StaffRoutes.registerPet(selectedOwner.id))
        },
        onBackClick = { navController.popBackStack() }
    )

    LaunchedEffect(searchText) {
        val normalizedSearch = searchText.trim()

        if (normalizedSearch.isBlank()) {
            owners = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        delay(300)
        isLoading = true
        errorMessage = ""

        try {
            // searchOwners already filters by role = pet_owner
            owners = GetProfileData.searchOwners(normalizedSearch)
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = "Unable to search owners."
            owners = emptyList()
        } finally {
            isLoading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOwnerContent(
    searchText: String,
    owners: List<Profile>,
    isLoading: Boolean,
    errorMessage: String,
    onSearchChange: (String) -> Unit,
    onOwnerClick: (Profile) -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Owner") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search owner by name or email") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                searchText.isBlank() -> {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = "Type a name or email to search for a pet owner.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                owners.isEmpty() -> {
                    Text(
                        text = "No owners found for \"$searchText\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(owners) { owner ->
                            OwnerCard(
                                owner = owner,
                                onClick = { onOwnerClick(owner) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OwnerCard(
    owner: Profile,
    onClick: () -> Unit
) {
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
                text = "👤",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = owner.full_name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = owner.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
