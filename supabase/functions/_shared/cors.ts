/**
 * CORS headers for Edge Function invocations from browsers.
 * Adjust `allowOrigin` for production if needed.
 */
export const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Max-Age": "86400",
} as const;

/**
 * Handles preflight OPTIONS requests.
 * Returns a 204 No Content with CORS headers if the request is OPTIONS.
 */
export function handleCors(req: Request): Response | null {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: corsHeaders });
  }
  return null;
}

/**
 * Adds CORS headers to an existing Response.
 */
export function withCors(res: Response): Response {
  const headers = new Headers(res.headers);
  for (const [k, v] of Object.entries(corsHeaders)) {
    headers.set(k, v);
  }
  return new Response(res.body, { status: res.status, headers });
}