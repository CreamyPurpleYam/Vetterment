/**
 * Edge Function: create-paypal-order
 *
 * Creates a PayPal order for a ₱400 PHP consultation appointment.
 * - Authenticates the user via JWT
 * - Validates pet ownership
 * - Records a payment attempt with a client-scoped idempotency key
 * - Generates a server-controlled PayPal-Request-Id for the PayPal API call
 * - Returns the order ID and approval URL
 *
 * Request body:
 *   { "petId": "<uuid>", "appointmentType": "CONSULTATION", "idempotencyKey": "<client-generated-uuid>" }
 *
 * Response:
 *   { "orderId": "<paypal-order-id>", "approvalUrl": "<url>" }
 *
 * Idempotency: same (owner_id, idempotencyKey) returns existing order without calling PayPal again.
 * Retries: if DB update fails after PayPal succeeds, retry reuses the server PayPal-Request-Id.
 */

import { handleCors, withCors } from "../_shared/cors.ts";
import { getUserIdFromReq, createServiceClient } from "../_shared/auth.ts";
import {
  createPayPalOrder,
  getApprovalUrl,
  type CreateOrderRequest,
  type PayPalOrder,
} from "../_shared/paypal.ts";

const EXPECTED_APPOINTMENT_TYPE = "CONSULTATION";
const AMOUNT_CENTS = 40000;
const CURRENCY = "PHP";
const RETURN_URL = Deno.env.get("PAYPAL_RETURN_URL") ?? "vetterment://payment-return";
const CANCEL_URL = Deno.env.get("PAYPAL_CANCEL_URL") ?? "vetterment://payment-cancel";

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
    const { userId, supabaseClient: userClient } = await getUserIdFromReq(req);

    // 2. Parse and validate request
    let body: { petId: string; appointmentType: string; idempotencyKey: string };
    try {
      body = await req.json();
    } catch {
      return errorResponse(400, "Invalid JSON body");
    }
    const { petId, appointmentType, idempotencyKey } = body;
    if (!petId || !appointmentType || !idempotencyKey) {
      return errorResponse(400, "Missing required fields: petId, appointmentType, idempotencyKey");
    }
    if (appointmentType !== EXPECTED_APPOINTMENT_TYPE) {
      return errorResponse(400, `Only ${EXPECTED_APPOINTMENT_TYPE} appointments are supported`);
    }

    // 3. Validate pet ownership (RLS-enforced via user client)
    const { data: pet, error: petError } = await userClient
      .from("pets")
      .select("id, owner_id, is_active")
      .eq("id", petId)
      .eq("owner_id", userId)
      .eq("is_active", true)
      .single();

    if (petError || !pet) {
      return errorResponse(404, "Pet not found or access denied");
    }

    // 4. Create service client for payment_attempts (bypasses RLS)
    const svc = createServiceClient();

    // 5. Upsert payment attempt with client-scoped idempotency key
    // The composite (owner_id, idempotency_key) ensures cross-owner isolation.
    // Requires unique index on (owner_id, idempotency_key) in payment_attempts table.
    const { data: existingAttempt, error: upsertError } = await svc
      .from("payment_attempts")
      .upsert({
        owner_id: userId,
        pet_id: petId,
        idempotency_key: idempotencyKey,
        amount_cents: AMOUNT_CENTS,
        currency: CURRENCY,
        status: "PENDING_ORDER",
      }, {
        onConflict: "owner_id,idempotency_key",
        ignoreDuplicates: false,
      })
      .select()
      .maybeSingle();

    if (upsertError) {
      // Check if it's a cross-owner conflict on the global idempotency_key unique index
      if (upsertError.code === "23505" && upsertError.message.includes("idempotency_key")) {
        return errorResponse(409, "Idempotency key already used");
      }
      console.error("Upsert payment attempt failed:", upsertError);
      return errorResponse(500, "Failed to record payment attempt");
    }

    if (!existingAttempt) {
      return errorResponse(500, "Failed to create or find payment attempt");
    }

    if (upsertError) {
      // Check if it's a cross-owner conflict on the global idempotency_key unique index
      if (upsertError.code === "23505" && upsertError.message.includes("idempotency_key")) {
        return errorResponse(409, "Idempotency key already used");
      }
      console.error("Upsert payment attempt failed:", upsertError);
      return errorResponse(500, "Failed to record payment attempt");
    }

    // 6. Determine if we need to call PayPal or return existing order
    let paypalOrder: PayPalOrder;
    let approvalUrl: string;

    if (existingAttempt.paypal_order_id) {
      // Existing order found - return it (idempotent retry)
      try {
        paypalOrder = await getPayPalOrder(existingAttempt.paypal_order_id);
        approvalUrl = getApprovalUrl(paypalOrder) ?? "";
        if (!approvalUrl) {
          // Fallback: reconstruct approval URL pattern (not guaranteed by PayPal after creation)
          approvalUrl = `${Deno.env.get("PAYPAL_ENV") === "live" ? "https://www.paypal.com" : "https://www.sandbox.paypal.com"}/checkoutnow?token=${existingAttempt.paypal_order_id}`;
        }
      } catch (e) {
        console.error("Failed to retrieve existing PayPal order:", e);
        return errorResponse(500, "Failed to retrieve existing payment order");
      }
    } else if (existingAttempt.paypal_request_id) {
      // Row exists but paypal_order_id not set - previous PayPal call may have succeeded
      // Reuse the stored PayPal-Request-Id to check if order was created
      try {
        // We need to find the order by the custom_id (which is the idempotencyKey)
        // Since we can't directly query PayPal by custom_id, we'll try to create with the same PayPal-Request-Id
        // If it conflicts, we'll fetch the existing order
        paypalOrder = await createPayPalOrder({
          intent: "CAPTURE",
          purchase_units: [{
            amount: { currency_code: CURRENCY, value: (AMOUNT_CENTS / 100).toFixed(2) },
            description: "VETterment Check-up Consultation",
            custom_id: idempotencyKey,
          }],
          application_context: {
            return_url: RETURN_URL,
            cancel_url: CANCEL_URL,
            brand_name: "PetHub Veterinary Clinic",
            locale: "en-PH",
            landing_page: "BILLING",
            shipping_preference: "NO_SHIPPING",
            user_action: "PAY_NOW",
          },
        }, existingAttempt.paypal_request_id);
      } catch (e: any) {
        if (e.message === "IDEMPOTENCY_CONFLICT") {
          // Order was already created with this PayPal-Request-Id
          // Find it using the idempotency key
          const { data: attemptWithOrder } = await svc
            .from("payment_attempts")
            .select("paypal_order_id")
            .eq("owner_id", userId)
            .eq("idempotency_key", idempotencyKey)
            .single();

          if (attemptWithOrder?.paypal_order_id) {
            paypalOrder = await getPayPalOrder(attemptWithOrder.paypal_order_id);
          } else {
            throw new Error("Idempotency conflict but no order found");
          }
        } else {
          throw e;
        }
      }

      approvalUrl = getApprovalUrl(paypalOrder);
      if (!approvalUrl) {
        await svc
          .from("payment_attempts")
          .update({ status: "FAILED", updated_at: new Date().toISOString() })
          .eq("owner_id", userId)
          .eq("idempotency_key", idempotencyKey);
        return errorResponse(500, "PayPal did not return an approval URL");
      }

      // Persist the order ID
      const { error: updateError } = await svc
        .from("payment_attempts")
        .update({
          paypal_order_id: paypalOrder.id,
          approval_url: approvalUrl,
          status: "CREATED",
          updated_at: new Date().toISOString(),
        })
        .eq("owner_id", userId)
        .eq("idempotency_key", idempotencyKey);

      if (updateError) {
        console.error("Failed to update payment attempt with PayPal order ID:", updateError);
      }
    } else {
      // 7. Generate server-controlled PayPal-Request-Id
      // Store it in the payment attempt so retries can reuse the same idempotency key
      const paypalRequestId = `order-${crypto.randomUUID()}`;

      // Pre-store the paypal_request_id so we can recover if DB update fails later
      const { error: preStoreError } = await svc
        .from("payment_attempts")
        .update({ paypal_request_id: paypalRequestId, updated_at: new Date().toISOString() })
        .eq("owner_id", userId)
        .eq("idempotency_key", idempotencyKey);

      if (preStoreError) {
        console.error("Failed to pre-store paypal_request_id:", preStoreError);
        // Continue anyway - not critical
      }

      // 8. Call PayPal to create order
      const createRequest: CreateOrderRequest = {
        intent: "CAPTURE",
        purchase_units: [{
          amount: { currency_code: CURRENCY, value: (AMOUNT_CENTS / 100).toFixed(2) },
          description: "VETterment Check-up Consultation",
          custom_id: idempotencyKey,
        }],
        application_context: {
          return_url: RETURN_URL,
          cancel_url: CANCEL_URL,
          brand_name: "PetHub Veterinary Clinic",
          locale: "en-PH",
          landing_page: "BILLING",
          shipping_preference: "NO_SHIPPING",
          user_action: "PAY_NOW",
        },
      };

      try {
        paypalOrder = await createPayPalOrder(createRequest, paypalRequestId);
      } catch (e: any) {
        if (e.message === "IDEMPOTENCY_CONFLICT") {
          // Race condition: another request with same PayPal-Request-Id succeeded
          // Fetch the order using the idempotency key from custom_id
          const { data: attempt } = await svc
            .from("payment_attempts")
            .select("paypal_order_id")
            .eq("owner_id", userId)
            .eq("idempotency_key", idempotencyKey)
            .single();

          if (attempt?.paypal_order_id) {
            paypalOrder = await getPayPalOrder(attempt.paypal_order_id);
          } else {
            throw new Error("Idempotency conflict but no order found");
          }
        } else {
          // Update attempt status to FAILED
          await svc
            .from("payment_attempts")
            .update({ status: "FAILED", updated_at: new Date().toISOString() })
            .eq("owner_id", userId)
            .eq("idempotency_key", idempotencyKey);
          throw e;
        }
      }

      // 9. Extract approval URL
      approvalUrl = getApprovalUrl(paypalOrder);
      if (!approvalUrl) {
        // Should not happen for new orders, but guard anyway
        await svc
          .from("payment_attempts")
          .update({ status: "FAILED", updated_at: new Date().toISOString() })
          .eq("owner_id", userId)
          .eq("idempotency_key", idempotencyKey);
        return errorResponse(500, "PayPal did not return an approval URL");
      }

      // 10. Persist PayPal order ID and approval URL
      const { error: updateError } = await svc
        .from("payment_attempts")
        .update({
          paypal_order_id: paypalOrder.id,
          approval_url: approvalUrl,
          status: "CREATED",
          updated_at: new Date().toISOString(),
        })
        .eq("owner_id", userId)
        .eq("idempotency_key", idempotencyKey);

      if (updateError) {
        console.error("Failed to update payment attempt with PayPal order ID:", updateError);
        // Do not return error here - PayPal order was created successfully
        // The retry logic will handle this on next call
      }
    }

    // 11. Return success
    return successResponse({
      orderId: paypalOrder.id,
      approvalUrl,
    });

  } catch (e: any) {
    console.error("create-paypal-order error:", e);
    return errorResponse(500, e.message || "Internal server error");
  }
});