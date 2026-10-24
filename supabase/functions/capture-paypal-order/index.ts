/**
 * Edge Function: capture-paypal-order
 *
 * Captures a PayPal order after buyer approval.
 * - Authenticates the user via JWT
 * - Verifies the payment attempt belongs to this owner
 * - Pre-capture: retrieves order and validates amount/currency/merchant
 * - Captures with idempotency (PayPal-Request-Id: capture-{orderId})
 * - Post-capture: validates completed capture response
 * - Persists capture result atomically
 * - Returns verified capture details
 *
 * Request body:
 *   { "orderId": "<paypal-order-id>" }
 *
 * Response:
 *   { "captured": true, "captureId": "<paypal-capture-id>", "amount": "400.00", "currency": "PHP" }
 *
 * Concurrency safety:
 * - Fast path: if already CAPTURED with capture_id, return it
 * - Update verifies row matches expected owner + orderId + not-already-captured
 * - Never reports success before capture is verified AND persisted
 */

import { handleCors, withCors } from "../_shared/cors.ts";
import { getUserIdFromReq, createServiceClient } from "../_shared/auth.ts";
import {
  getPayPalOrder,
  capturePayPalOrder,
  validateOrderForCapture,
  validateCaptureResponse,
  type PayPalOrder,
  type CaptureResponse,
} from "../_shared/paypal.ts";

const EXPECTED_MERCHANT_ID = Deno.env.get("EXPECTED_MERCHANT_ID") || undefined;

function errorResponse(status: number, message: string) {
  return withCors(new Response(JSON.stringify({ error: message }), {
    status,
    headers: { "Content-Type": "application/json" },
  }));
}

function successResponse(data: object) {
  return withCors(new Response(JSON.stringify(data), {
    status: 200,
    headers: { "Content-Type": "application/json" },
  }));
}

Deno.serve(async (req: Request) => {
  // Handle CORS preflight
  const corsResp = handleCors(req);
  if (corsResp) return corsResp;

  if (req.method !== "POST") {
    return errorResponse(405, "Method not allowed");
  }

  try {
    // 1. Authenticate user
    const { userId } = await getUserIdFromReq(req);

    // 2. Parse request
    let body: { orderId: string };
    try {
      body = await req.json();
    } catch {
      return errorResponse(400, "Invalid JSON body");
    }
    const { orderId } = body;
    if (!orderId) {
      return errorResponse(400, "Missing orderId");
    }

    // 3. Create service client for payment_attempts
    const svc = createServiceClient();

    // 4. Look up payment attempt - verifies ownership
    const { data: attempt, error: lookupError } = await svc
      .from("payment_attempts")
      .select("*")
      .eq("paypal_order_id", orderId)
      .eq("owner_id", userId)
      .single();

    if (lookupError || !attempt) {
      return errorResponse(404, "Payment attempt not found or access denied");
    }

    // 5. Fast path: already captured and persisted
    if (attempt.status === "CAPTURED" && attempt.capture_id) {
      return successResponse({
        captured: true,
        captureId: attempt.capture_id,
        amount: (attempt.amount_cents / 100).toFixed(2),
        currency: attempt.currency,
      });
    }

    // 6. Pre-capture validation: retrieve order from PayPal
    let order: PayPalOrder;
    try {
      order = await getPayPalOrder(orderId);
    } catch (e: any) {
      console.error("Failed to retrieve PayPal order:", e);
      return errorResponse(500, "Failed to retrieve order from PayPal");
    }

    // 7. Validate order matches our expectations
    try {
      validateOrderForCapture(order, EXPECTED_MERCHANT_ID);
    } catch (e: any) {
      // Update attempt status to reflect validation failure
      await svc
        .from("payment_attempts")
        .update({ status: "FAILED", updated_at: new Date().toISOString() })
        .eq("paypal_order_id", orderId)
        .eq("owner_id", userId);
      return errorResponse(400, e.message);
    }

    // 8. Capture with idempotency key scoped to this order
    const captureRequestId = `capture-${orderId}`;
    let captureResp: CaptureResponse;
    try {
      captureResp = await capturePayPalOrder(orderId, captureRequestId);
    } catch (e: any) {
      if (e.message === "IDEMPOTENCY_CONFLICT") {
        // Another capture call with same idempotency key succeeded
        // Re-fetch order to get the existing capture
        try {
          const refreshedOrder = await getPayPalOrder(orderId);
          const capture = refreshedOrder.purchase_units?.[0]?.payments?.captures?.[0];
          if (capture?.status === "COMPLETED") {
            // Persist the existing capture
            const validated = validateCaptureResponse({
              purchase_units: [{ payments: { captures: [capture] } }]
            } as CaptureResponse);
            await svc
              .from("payment_attempts")
              .update({
                status: "CAPTURED",
                capture_id: validated.captureId,
                paypal_response: { capture },
                updated_at: new Date().toISOString(),
              })
              .eq("paypal_order_id", orderId)
              .eq("owner_id", userId);

            return successResponse({
              captured: true,
              captureId: validated.captureId,
              amount: validated.amount,
              currency: validated.currency,
            });
          }
        } catch (refetchError) {
          console.error("Failed to refetch order after idempotency conflict:", refetchError);
        }
      }
      console.error("PayPal capture failed:", e);
      return errorResponse(500, "Failed to capture payment");
    }

    // 9. Validate capture response independently
    let validatedCapture;
    try {
      validatedCapture = validateCaptureResponse(captureResp);
    } catch (e: any) {
      await svc
        .from("payment_attempts")
        .update({ status: "FAILED", updated_at: new Date().toISOString() })
        .eq("paypal_order_id", orderId)
        .eq("owner_id", userId);
      return errorResponse(400, e.message);
    }

    // 10. Persist capture result - concurrency-safe update
    const { data: updatedRows, error: updateError } = await svc
      .from("payment_attempts")
      .update({
        status: "CAPTURED",
        capture_id: validatedCapture.captureId,
        paypal_response: { capture: captureResp.purchase_units?.[0]?.payments?.captures?.[0] },
        updated_at: new Date().toISOString(),
      })
      .eq("paypal_order_id", orderId)
      .eq("owner_id", userId)
      .neq("status", "CAPTURED") // Only update if not already captured
      .select();

    if (updateError) {
      console.error("Failed to persist capture result:", updateError);
      // The capture succeeded at PayPal but DB update failed
      // Return error - the retry will hit the fast path (step 5)
      return errorResponse(500, "Capture succeeded but failed to record result. Please retry.");
    }

    // Verify the update actually affected a row
    if (!updatedRows || updatedRows.length === 0) {
      // Another concurrent request may have already captured
      const { data: refreshed } = await svc
        .from("payment_attempts")
        .select("capture_id, amount_cents, currency")
        .eq("paypal_order_id", orderId)
        .eq("owner_id", userId)
        .single();

      if (refreshed?.capture_id) {
        return successResponse({
          captured: true,
          captureId: refreshed.capture_id,
          amount: (refreshed.amount_cents / 100).toFixed(2),
          currency: refreshed.currency,
        });
      }
      return errorResponse(500, "Capture persisted by another request but capture ID not found");
    }

    // 11. Success
    return successResponse({
      captured: true,
      captureId: validatedCapture.captureId,
      amount: validatedCapture.amount,
      currency: validatedCapture.currency,
    });

  } catch (e: any) {
    console.error("capture-paypal-order error:", e);
    return errorResponse(500, e.message || "Internal server error");
  }
});