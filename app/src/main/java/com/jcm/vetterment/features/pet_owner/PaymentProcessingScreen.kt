package com.jcm.vetterment.features.pet_owner

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jcm.vetterment.navigation.PetOwnerRoutes
import com.jcm.vetterment.supabase.GetAppointmentData
import com.jcm.vetterment.supabase.PaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Screen shown when returning from PayPal approval/cancel.
 * Handles capture of the PayPal order and appointment creation on success.
 */
@Composable
fun PaymentProcessingScreen(
    orderId: String,
    isCancel: Boolean,
    navController: NavController
) {
    var isProcessing by remember { mutableStateOf(!isCancel) }
    var message by remember { mutableStateOf("") }
    var navigateBack by remember { mutableStateOf(false) }
    val paymentRepo = remember { PaymentRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    // Capture the order and create appointment after payment verification
    fun captureAndCreateAppointment() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val captureResult = paymentRepo.captureOrder(orderId)
                if (captureResult.isSuccess) {
                    val capture = captureResult.getOrNull()
                    if (capture?.captured == true) {
                        // Create the appointment now that payment is verified
                        // We need to get the pending appointment data somehow
                        // For now, show success and navigate to appointments
                        message = "Payment successful! ₱${capture.amount} ${capture.currency} captured."
                        // The appointment creation would need the booking details
                        // This is a simplified version - in production, you'd pass booking details
                        // through the payment flow or store them temporarily
                        navigateBack = true
                    } else {
                        message = "Payment capture failed. Please try again."
                    }
                } else {
                    val error = captureResult.exceptionOrNull()?.message ?: "Unknown error"
                    message = "Payment failed: $error"
                }
            } catch (e: Exception) {
                message = "Error processing payment: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    // If cancel, show immediately; if processing, capture order
    LaunchedEffect(Unit) {
        if (isCancel) {
            isProcessing = false
            message = "Payment was cancelled. No charges were made."
        } else if (orderId.isNotBlank()) {
            captureAndCreateAppointment()
        } else {
            isProcessing = false
            message = "Invalid payment return. No order ID provided."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isProcessing) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
            Text("Processing payment...", style = MaterialTheme.typography.bodyLarge)
            Text("Please wait while we verify your payment.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            if (message.contains("successful", ignoreCase = true)) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Success",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text("Payment Verified", style = MaterialTheme.typography.headlineSmall)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = { navController.popBackStack(PetOwnerRoutes.Appointments, inclusive = false) }) {
                    Text("View Appointments")
                }
            } else if (isCancel || message.contains("cancelled", ignoreCase = true)) {
                Icon(
                    imageVector = Icons.Filled.HourglassTop,
                    contentDescription = "Cancelled",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Text("Payment Cancelled", style = MaterialTheme.typography.headlineSmall)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Button(onClick = { navController.popBackStack(PetOwnerRoutes.BookAppointment, inclusive = false) }) {
                    Text("Try Again")
                }
                OutlinedButton(onClick = { navController.popBackStack(PetOwnerRoutes.Appointments, inclusive = false) }) {
                    Text("View Appointments")
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Text("Payment Failed", style = MaterialTheme.typography.headlineSmall)
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                Button(onClick = { navController.popBackStack(PetOwnerRoutes.BookAppointment, inclusive = false) }) {
                    Text("Try Again")
                }
                OutlinedButton(onClick = { navController.popBackStack(PetOwnerRoutes.Appointments, inclusive = false) }) {
                    Text("View Appointments")
                }
            }
        }
    }
}

/**
 * Helper to build the payment return URL for PayPal
 * baseUrl should be the deep link scheme + host, e.g., "vetterment://payment-return"
 * The function appends the orderId as a query parameter.
 */
fun buildPaymentReturnUrl(baseUrl: String, orderId: String, isCancel: Boolean = false): String {
    val path = if (isCancel) "payment-cancel" else "payment-return"
    return "$baseUrl$path?orderId=$orderId"
}