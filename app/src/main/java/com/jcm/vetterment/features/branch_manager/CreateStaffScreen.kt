package com.jcm.vetterment.features.branchmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.auth.formatName
import com.jcm.vetterment.supabase.GetProfileData

// ─── Logic layer ──────────────────────────────────────────────────────────────

@Composable
fun CreateStaffScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("") }

    var fullNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }
    var roleError by remember { mutableStateOf("") }
    var submitError by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var createdName by remember { mutableStateOf("") }

    CreateStaffContent(
        fullName = fullName,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        selectedRole = selectedRole,
        fullNameError = fullNameError,
        emailError = emailError,
        passwordError = passwordError,
        confirmPasswordError = confirmPasswordError,
        roleError = roleError,
        submitError = submitError,
        isSubmitting = isSubmitting,
        onFullNameChange = { fullName = it; fullNameError = "" },
        onEmailChange = { email = it; emailError = "" },
        onPasswordChange = { password = it; passwordError = "" },
        onConfirmPasswordChange = { confirmPassword = it; confirmPasswordError = "" },
        onRoleChange = { selectedRole = it; roleError = "" },
        onBackClick = { navController.popBackStack() },
        onCreateClick = {
            // Validate
            var valid = true
            val formattedName = formatName(fullName)

            if (formattedName.isBlank()) {
                fullNameError = "Full name is required."
                valid = false
            }
            if (email.isBlank()) {
                emailError = "Email is required."
                valid = false
            }
            if (password.length < 8) {
                passwordError = "Password must be at least 8 characters."
                valid = false
            }
            if (password != confirmPassword) {
                confirmPasswordError = "Passwords do not match."
                valid = false
            }
            if (selectedRole.isBlank()) {
                roleError = "Please select a role."
                valid = false
            }
            if (valid) isSubmitting = true
        }
    )

    // Submission
    LaunchedEffect(isSubmitting) {
        if (!isSubmitting) return@LaunchedEffect
        submitError = ""
        try {
            val result = GetProfileData.createStaffAccount(
                fullName = formatName(fullName),
                email = email.trim().lowercase(),
                password = password,
                role = selectedRole
            )
            if (result != null) {
                createdName = formatName(fullName)
                showSuccessDialog = true
            } else {
                submitError = "Failed to create account. The email may already be registered."
            }
        } catch (e: Exception) {
            e.printStackTrace()
            submitError = e.message?.let {
                when {
                    it.contains("already registered") -> "This email is already registered."
                    it.contains("Invalid role") -> "Invalid role selected."
                    else -> "Something went wrong. Please try again."
                }
            } ?: "Something went wrong. Please try again."
        } finally {
            isSubmitting = false
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Account Created") },
            text = {
                Text(
                    "Staff account for $createdName has been created successfully. " +
                    "They can now log in with the provided credentials."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    navController.popBackStack()
                }) { Text("Done") }
            }
        )
    }
}

// ─── UI layer ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStaffContent(
    fullName: String,
    email: String,
    password: String,
    confirmPassword: String,
    selectedRole: String,
    fullNameError: String,
    emailError: String,
    passwordError: String,
    confirmPasswordError: String,
    roleError: String,
    submitError: String,
    isSubmitting: Boolean,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onCreateClick: () -> Unit
) {
    val roles = listOf(
        "vet" to "Veterinarian",
        "assistant_vet" to "Assistant Veterinarian",
        "nurse" to "Nurse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Staff Account") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "* Required fields",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (submitError.isNotBlank()) {
                Text(
                    text = submitError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── Full name ─────────────────────────────────────────────────
            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text("Full Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = fullNameError.isNotBlank(),
                supportingText = if (fullNameError.isNotBlank()) {
                    { Text(fullNameError) }
                } else null
            )

            // ── Email ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = emailError.isNotBlank(),
                supportingText = if (emailError.isNotBlank()) {
                    { Text(emailError) }
                } else null
            )

            // ── Role dropdown ─────────────────────────────────────────────
            RoleDropdown(
                selectedRole = selectedRole,
                roles = roles,
                onRoleChange = onRoleChange,
                isError = roleError.isNotBlank(),
                errorText = roleError
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Set Password",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // ── Password ──────────────────────────────────────────────────
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = passwordError.isNotBlank(),
                supportingText = if (passwordError.isNotBlank()) {
                    { Text(passwordError) }
                } else null
            )

            // ── Confirm password ──────────────────────────────────────────
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                isError = confirmPasswordError.isNotBlank(),
                supportingText = if (confirmPasswordError.isNotBlank()) {
                    { Text(confirmPasswordError) }
                } else null
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Create button ─────────────────────────────────────────────
            Button(
                onClick = onCreateClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating…")
                } else {
                    Text("Create Account")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdown(
    selectedRole: String,
    roles: List<Pair<String, String>>,
    onRoleChange: (String) -> Unit,
    isError: Boolean,
    errorText: String
) {
    var expanded by remember { mutableStateOf(false) }
    val displayName = roles.find { it.first == selectedRole }?.second ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Role *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            isError = isError,
            supportingText = if (isError && errorText.isNotBlank()) {
                { Text(errorText) }
            } else null
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            roles.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onRoleChange(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
