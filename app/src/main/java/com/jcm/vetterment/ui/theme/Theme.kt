package com.jcm.vetterment.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = White,
    primaryContainer = PrimaryGreen,
    onPrimaryContainer = White,
    secondary = ButtonGreen,
    onSecondary = Black,
    tertiary = PrimaryGreen
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = White,
    primaryContainer = PrimaryGreen,
    onPrimaryContainer = White,
    secondary = ButtonGreen,
    onSecondary = Black,
    tertiary = PrimaryGreen
)

@Composable
fun VettermentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun vettermentTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF1B1B1B),
    unfocusedTextColor = Color(0xFF1B1B1B),
    focusedBorderColor = PrimaryGreen,
    unfocusedBorderColor = DarkGreen,
    focusedLabelColor = PrimaryGreen,
    unfocusedLabelColor = DarkGreen,
    cursorColor = PrimaryGreen,
    focusedTrailingIconColor = DarkGreen,
    unfocusedTrailingIconColor = DarkGreen,
    focusedPlaceholderColor = Color(0xFF757575),
    unfocusedPlaceholderColor = Color(0xFF757575)
)

@Composable
fun vettermentButtonColors() = ButtonDefaults.buttonColors(
    containerColor = PrimaryGreen,
    contentColor = White
)
