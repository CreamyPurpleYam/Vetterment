/**
 * PayPal API client for Edge Functions.
 * Handles OAuth token, order creation, retrieval, and capture with idempotency.
 */

const PAYPAL_API_BASE = (() => {
  const env = Deno.env.get("PAYPAL_ENV")?.toLowerCase() ?? "sandbox";
  return env === "live"
    ? "https://api-m.paypal.com"
    : "https://api-m.sandbox.paypal.com";
})();

interface PayPalTokenResponse {
  access_token: string;
  token_type: string;
  expires_in: number;
}

let cachedToken: { token: string; expiresAt: number } | null = null;

/**
 * Fetches or returns cached OAuth access token.
 */
async function getAccessToken(): Promise<string> {
  const now = Date.now();
  if (cachedToken && cachedToken.expiresAt > now + 60_000) {
    return cachedToken.token;
  }

  const clientId = Deno.env.get("PAYPAL_CLIENT_ID");
  const clientSecret = Deno.env.get("PAYPAL_CLIENT_SECRET");
  if (!clientId || !clientSecret) {
    throw new Error("PayPal credentials not configured");
  }

  const resp = await fetch(`${PAYPAL_API_BASE}/v1/oauth2/token`, {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      Authorization: "Basic " + btoa(`${clientId}:${clientSecret}`),
    },
    body: "grant_type=client_credentials",
  });

  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`PayPal token error ${resp.status}: ${text}`);
  }

  const data = (await resp.json()) as PayPalTokenResponse;
  cachedToken = { token: data.access_token, expiresAt: now + data.expires_in * 1000 };
  return data.access_token;
}

export interface CreateOrderRequest {
  intent: "CAPTURE";
  purchase_units: Array<{
    amount: { currency_code: "PHP"; value: "400.00" };
    description?: string;
    custom_id?: string; // our payment_attempt idempotency key
  }>;
  application_context?: {
    return_url?: string;
    cancel_url?: string;
    brand_name?: string;
    locale?: string;
    landing_page?: "BILLING" | "LOGIN" | "NO_PREFERENCE";
    shipping_preference?: "NO_SHIPPING";
    user_action?: "PAY_NOW";
  };
}

export interface PayPalOrder {
  id: string;
  status: "CREATED" | "SAVED" | "APPROVED" | "VOIDED" | "COMPLETED" | "PAYER_ACTION_REQUIRED";
  intent: string;
  purchase_units: Array<{
    amount: { currency_code: string; value: string };
    payee?: { merchant_id: string };
    payments?: {
      captures?: Array<{
        id: string;
        status: string;
        amount: { currency_code: string; value: string };
        final_capture: boolean;
      }>;
    };
  }>;
  links: Array<{ rel: string; href: string; method: string }>;
}

export interface CaptureResponse {
  id: string;
  status: string;
  purchase_units: Array<{
    payments: {
      captures: Array<{
        id: string;
        status: string;
        amount: { currency_code: string; value: string };
        final_capture: boolean;
      }>;
    };
  }>;
}

/**
 * Creates a PayPal order with idempotency via PayPal-Request-Id header.
 */
export async function createPayPalOrder(
  request: CreateOrderRequest,
  paypalRequestId: string
): Promise<PayPalOrder> {
  const token = await getAccessToken();
  const resp = await fetch(`${PAYPAL_API_BASE}/v2/checkout/orders`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      "PayPal-Request-Id": paypalRequestId,
      Prefer: "return=representation",
    },
    body: JSON.stringify(request),
  });

  if (!resp.ok) {
    const text = await resp.text();
    // Check for specific idempotency conflict
    if (resp.status === 422) {
      try {
        const err = JSON.parse(text);
        if (err.details?.some((d: any) => d.issue === "DUPLICATE_REQUEST_ID")) {
          throw new Error("IDEMPOTENCY_CONFLICT");
        }
      } catch {}
    }
    throw new Error(`PayPal create order failed ${resp.status}: ${text}`);
  }

  return (await resp.json()) as PayPalOrder;
}

/**
 * Retrieves a PayPal order by ID.
 */
export async function getPayPalOrder(orderId: string): Promise<PayPalOrder> {
  const token = await getAccessToken();
  const resp = await fetch(`${PAYPAL_API_BASE}/v2/checkout/orders/${orderId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`PayPal get order failed ${resp.status}: ${text}`);
  }
  return (await resp.json()) as PayPalOrder;
}

/**
 * Captures a PayPal order with idempotency via PayPal-Request-Id header.
 * Returns the full capture response.
 */
export async function capturePayPalOrder(
  orderId: string,
  paypalRequestId: string
): Promise<CaptureResponse> {
  const token = await getAccessToken();
  const resp = await fetch(`${PAYPAL_API_BASE}/v2/checkout/orders/${orderId}/capture`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      "PayPal-Request-Id": paypalRequestId,
      Prefer: "return=representation",
    },
  });

  if (!resp.ok) {
    const text = await resp.text();
    // Some idempotency conflicts may return 422
    if (resp.status === 422) {
      try {
        const err = JSON.parse(text);
        if (err.details?.some((d: any) => d.issue === "DUPLICATE_REQUEST_ID")) {
          throw new Error("IDEMPOTENCY_CONFLICT");
        }
      } catch {}
    }
    throw new Error(`PayPal capture failed ${resp.status}: ${text}`);
  }

  return (await resp.json()) as CaptureResponse;
}

/**
 * Extracts the approval URL from a PayPal order's links.
 */
export function getApprovalUrl(order: PayPalOrder): string | null {
  const link = order.links.find((l) => l.rel === "approve" || l.rel === "payer-action");
  return link?.href ?? null;
}

/**
 * Validates that a PayPal order matches our expected amount, currency, and intent.
 */
export function validateOrderForCapture(order: PayPalOrder, expectedMerchantId?: string): void {
  if (order.intent !== "CAPTURE") throw new Error("Order intent is not CAPTURE");
  if (!["CREATED", "APPROVED"].includes(order.status)) {
    throw new Error(`Order not capturable: ${order.status}`);
  }
  const pu = order.purchase_units[0];
  if (!pu) throw new Error("Order has no purchase units");
  if (pu.amount?.currency_code !== "PHP") throw new Error(`Currency mismatch: ${pu.amount?.currency_code}`);
  if (Number(pu.amount?.value) !== 400.00) throw new Error(`Amount mismatch: ${pu.amount?.value}`);
  if (expectedMerchantId && pu.payee?.merchant_id !== expectedMerchantId) {
    throw new Error("Payee merchant ID mismatch");
  }
}

/**
 * Validates a completed capture response.
 */
export function validateCaptureResponse(captureResp: CaptureResponse): {
  captureId: string;
  amount: string;
  currency: string;
} {
  const capture = captureResp.purchase_units?.[0]?.payments?.captures?.[0];
  if (!capture) throw new Error("No capture in response");
  if (capture.status !== "COMPLETED") throw new Error(`Capture not completed: ${capture.status}`);
  if (capture.amount?.currency_code !== "PHP") throw new Error(`Capture currency mismatch: ${capture.amount?.currency_code}`);
  if (Number(capture.amount?.value) !== 400.00) throw new Error(`Capture amount mismatch: ${capture.amount?.value}`);
  return { captureId: capture.id, amount: capture.amount.value, currency: capture.amount.currency_code };
}