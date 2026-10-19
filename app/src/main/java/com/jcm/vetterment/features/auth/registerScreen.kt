package com.jcm.vetterment.features.auth

import com.jcm.vetterment.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jcm.vetterment.features.components.VettermentBody
import com.jcm.vetterment.features.components.VettermentButton
import com.jcm.vetterment.features.components.VettermentError
import com.jcm.vetterment.features.components.VettermentOutlinedTextField
import com.jcm.vetterment.features.components.VettermentSecondary
import com.jcm.vetterment.features.components.VettermentTitle
import kotlinx.coroutines.launch
import com.jcm.vetterment.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import com.jcm.vetterment.features.data_models.Profile

@Composable
fun RegisterScreen( //Logic Layer
    navController: NavController
) {

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val cleanedEmail = email.trim().lowercase()

    val scope = rememberCoroutineScope()

    RegisterContent(
        fullName = fullName,
        email = email,
        password = password,
        errorMessage = errorMessage,

        onFullNameChange = {
            fullName = it
        },

        onEmailChange = {
            email = it
        },

        onPasswordChange = {
            password = it
        },

        confirmPassword = confirmPassword,
        onConfirmPasswordChange = {
            confirmPassword = it
        },

        onRegisterClick = {
            errorMessage = ""
            val formattedName = formatName(fullName)

            when {
                formattedName.isBlank() ->
                    errorMessage = "Please enter your full name"
                email.isBlank() ->
                    errorMessage = "Please enter your email"
                password.length < 8 ->
                    errorMessage = "Password must be at least 8 characters"
                password != confirmPassword ->
                    errorMessage = "Passwords do not match"
                else -> {
                    scope.launch {
                        try {
                            println("FULLNAME = [$formattedName]")
                            println("EMAIL = [$email]")
                            println("EMAIL LENGTH = ${email.length}")
                            SupabaseClient.client.auth.signUpWith(Email){
                                this.email = cleanedEmail
                                this.password = password
                            }
                            val user =
                                SupabaseClient.client.auth.currentUserOrNull()

                            if (user != null) {

                                val profile = Profile(
                                    id = user.id,
                                    full_name = formattedName,
                                    email = cleanedEmail,
                                    role = "pet_owner"
                                )

                                SupabaseClient.client
                                    .from("profiles")
                                    .insert(profile)
                            }

                            println("PROFILE CREATED")

                            navController.popBackStack()
                        } catch (e: Exception){
                            errorMessage = when {

                                e.message?.contains("email_address_invalid") == true ->
                                    "Please enter a valid email address."

                                e.message?.contains("already registered") == true ->
                                    "This email is already registered."

                                e.message?.contains("Invalid login credentials") == true ->
                                    "Incorrect email or password."

                                else ->
                                    "Something went wrong. Please try again."
                            }
                            /*e.printStackTrace() //debug
                            errorMessage = e.message ?: "Unknown error"*/
                        }
                    }
                }
            }
        },

        onLoginClick = {
            navController.popBackStack()
        }
    )
}


@Composable
fun RegisterContent(
    fullName: String,
    email: String,
    password: String,
    confirmPassword: String,
    errorMessage: String,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        val headerHeight = maxHeight * 0.36f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BANNER IMAGE
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "PetHub Background",
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .offset(y = (-120).dp)
            )

            // REGISTRATION FORM
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-80).dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                VettermentTitle("REGISTER")

                Spacer(modifier = Modifier.height(15.dp))

                if (errorMessage.isNotEmpty()) {
                    VettermentError(errorMessage)

                    Spacer(modifier = Modifier.height(16.dp))
                }

                VettermentOutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = "Full Name"
                )

                Spacer(modifier = Modifier.height(12.dp))

                VettermentOutlinedTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = "Email"
                )

                Spacer(modifier = Modifier.height(12.dp))

                VettermentOutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(12.dp))

                VettermentOutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirm Password",
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                VettermentButton(
                    text = "REGISTER",
                    onClick = onRegisterClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                VettermentBody(
                    text = "Already have an account? Login",
                    modifier = Modifier.clickable {
                        onLoginClick()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview(showBackground = true) //Preview Layer
@Composable
fun RegisterPreview() {

    RegisterContent(
        fullName = "",
        email = "",
        password = "",
        confirmPassword = "",

        errorMessage = "",

        onFullNameChange = {},
        onEmailChange = {},
        onPasswordChange = {},
        onConfirmPasswordChange = {},

        onRegisterClick = {},
        onLoginClick = {}
    )
}

//Methods for Logic Layer
fun formatName(name: String): String {
    return name
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") {
            it.lowercase().replaceFirstChar { char ->
                char.uppercase()
            }
        }
}