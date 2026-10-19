package com.jcm.vetterment.features

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun BranchManager_Dashboard(navController: NavController) {
    Text(
        text = "Branch Manager",
        fontSize = 20.sp
    )
}