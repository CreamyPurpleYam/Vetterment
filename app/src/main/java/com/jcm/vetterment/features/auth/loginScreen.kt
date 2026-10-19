package com.jcm.vetterment.features.auth

import com.jcm.vetterment.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import com.jcm.vetterment.navigation.StaffRoutes
import com.jcm.vetterment.supabase.GetProfileData
import com.jcm.vetterment.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch


@Composable //Logic layer
fun LoginScreen(navController: NavController)
{
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    LoginContent(
        email = email,
        password = password,
        errorMessage = errorMessage,

        onEmailChange = {
            email = it
        },

        onPasswordChange = {
            password = it
        },

        onLoginClick = {
            errorMessage = ""
            when {
                email.isBlank() ->
                    errorMessage = "Please enter your email"
                password.isBlank() ->
                    errorMessage = "Please enter your password"
                else -> {
                    scope.launch {
                        try {
                            SupabaseClient.client.auth.signInWith(Email){
                                this.email = email
                                this.password = password
                            }
                            val role = GetProfileData.getCurrentRole()
                            when(role) {

                                "pet_owner" -> {
                                    navController.navigate("pet_owner") {
                                        popUpTo("login") {
                                            inclusive = true
                                        }
                                    }
                                }

                                "vet",
                                "assistant_vet",
                                "nurse" -> {
                                    navController.navigate(StaffRoutes.Dashboard) {
                                        popUpTo("login") {
                                            inclusive = true
                                        }
                                    }
                                }

                                "branch_manager" -> {
                                    navController.navigate("branch_manager") {
                                        popUpTo("login") {
                                            inclusive = true
                                        }
                                    }
                                }

                                else -> {
                                    errorMessage = "Unable to determine user role."
                                }
                            }
                        } catch (e: Exception){
                            errorMessage = when {
                                e.message?.contains("Invalid login credentials") == true ->
                                    "Incorrect email or password"
                                else ->
                                    "Login failed. Please try again later"
                            }
                            /*e.printStackTrace()
                            errorMessage = e.message ?: "Unknown error"*/
                        }
                    }
                }
            }
        },

        onRegisterClick = {
            navController.navigate("register")
        }
    )
}

@Composable
//UI layer
fun LoginContent(
    email: String,
    password: String,
    errorMessage: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
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
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "PetHub Background",
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
            )

            // LOGIN FORM
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                VettermentTitle("LOGIN")

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage.isNotEmpty()) {
                    VettermentError(errorMessage)
                    Spacer(modifier = Modifier.height(16.dp))
                }

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
                    isPassword = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(24.dp))

                VettermentButton(
                    text = "LOGIN",
                    onClick = onLoginClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                VettermentBody(
                    text = "Create Account",
                    modifier = Modifier.clickable {
                        onRegisterClick()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

}



@Preview(showBackground = true) //Preview UI
@Composable
fun LoginPreview() {

    LoginContent(
        email = "",
        password = "",
        errorMessage = "",

        onEmailChange = {},
        onPasswordChange = {},

        onLoginClick = {},
        onRegisterClick = {}
    )
}
