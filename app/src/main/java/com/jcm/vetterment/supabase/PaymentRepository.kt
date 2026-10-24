package com.jcm.vetterment.supabase

import android.util.Log
import com.jcm.vetterment.features.data_models.Pet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// PayPal integration temporarily disabled - uncomment when Supabase Functions are configured
// import io.github.jan.supabase.functions.functions
// import io.ktor.client.request.contentType
// import io.ktor.client.request.setBody
// import io.ktor.client.statement.bodyAsText
// import io.ktor.http.ContentType
// import io.ktor.http.HttpMethod

private const val TAG = "PaymentRepository"
private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class CreateOrderRequest(
    val petId: String,
    val appointmentType: String,
    val idempotencyKey: String
)

@Serializable
data class CreateOrderResponse(
    val orderId: String,
    val approvalUrl: String
)

@Serializable
data class CaptureOrderRequest(
    val orderId: String
)

@Serializable
data class CaptureOrderResponse(
    val captured: Boolean,
    val captureId: String,
    val amount: String,
    val currency: String
)

@Serializable
data class PaymentError(
    val error: String
)

/**
 * Repository for PayPal payment operations via Supabase Edge Functions.
 * PayPal integration temporarily disabled - stub implementations return failure.
 * Uncomment the actual implementation when Supabase Edge Functions are configured.
 */
class PaymentRepository private constructor() {

    companion object {
        @Volatile private var INSTANCE: PaymentRepository? = null

        fun getInstance(): PaymentRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PaymentRepository().also { INSTANCE = it }
            }
        }

        fun reset() {
            INSTANCE = null
        }
    }

    /**
     * Creates a PayPal order for a consultation appointment.
     * STUB: Returns failure - PayPal integration not yet configured.
     */
    suspend fun createOrder(
        pet: Pet,
        idempotencyKey: String
    ): Result<CreateOrderResponse> = withContext(Dispatchers.IO) {
        Log.w(TAG, "createOrder called but PayPal integration is disabled")
        Result.failure(Exception("PayPal integration not configured. Enable in PaymentRepository.kt"))
    }

    /**
     * Captures a PayPal order after buyer approval.
     * STUB: Returns failure - PayPal integration not yet configured.
     */
    suspend fun captureOrder(
        orderId: String
    ): Result<CaptureOrderResponse> = withContext(Dispatchers.IO) {
        Log.w(TAG, "captureOrder called but PayPal integration is disabled")
        Result.failure(Exception("PayPal integration not configured. Enable in PaymentRepository.kt"))
    }

    /*
    // ─── Actual PayPal implementation (commented out until Supabase Functions configured) ───

    suspend fun createOrder(
        pet: Pet,
        idempotencyKey: String
    ): Result<CreateOrderResponse> = withContext(Dispatchers.IO) {
        val request = CreateOrderRequest(
            petId = pet.id!!,
            appointmentType = "CONSULTATION",
            idempotencyKey = idempotencyKey
        )

        try {
            val response = SupabaseClient.client.functions.invoke("create-paypal-order") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            val responseBody = response.bodyAsText()
            if (response.status.value in 200..299) {
                val result = json.decodeFromString<CreateOrderResponse>(responseBody)
                Result.success(result)
            } else {
                val error = json.decodeFromString<PaymentError>(responseBody)
                Log.e(TAG, "Create order failed: ${response.status.value} - ${error.error}")
                Result.failure(Exception(error.error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create order exception", e)
            Result.failure(e)
        }
    }

    suspend fun captureOrder(
        orderId: String
    ): Result<CaptureOrderResponse> = withContext(Dispatchers.IO) {
        val request = CaptureOrderRequest(orderId = orderId)

        try {
            val response = SupabaseClient.client.functions.invoke("capture-paypal-order") {
                method = HttpMethod.Post
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
            val responseBody = response.bodyAsText()
            if (response.status.value in 200..299) {
                val result = json.decodeFromString<CaptureOrderResponse>(responseBody)
                Result.success(result)
            } else {
                val error = json.decodeFromString<PaymentError>(responseBody)
                Log.e(TAG, "Capture order failed: ${response.status.value} - ${error.error}")
                Result.failure(Exception(error.error))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Capture order exception", e)
            Result.failure(e)
        }
    }
    */
}