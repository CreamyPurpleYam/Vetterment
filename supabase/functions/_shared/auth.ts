/**
 * Authentication helpers for Edge Functions.
 * Extracts and validates the user's JWT from the Authorization header.
 */

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

export interface AuthResult {
  userId: string;
  supabaseClient: ReturnType<typeof createClient>;
}

/**
 * Creates a Supabase client using the caller's JWT (for RLS-enforced queries).
 * Returns the authenticated user's ID.
 */
export async function getUserIdFromReq(req: Request): Promise<AuthResult> {
  const authHeader = req.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    throw new Error("Missing or invalid Authorization header");
  }
  const jwt = authHeader.slice(7);

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  if (!supabaseUrl || !supabaseAnonKey) {
    throw new Error("Supabase URL or anon key not configured");
  }

  const supabase = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: `Bearer ${jwt}` } },
    auth: { persistSession: false },
  });

  const { data: { user }, error } = await supabase.auth.getUser(jwt);
  if (error || !user) {
    throw new Error("Invalid or expired token");
  }

  return { userId: user.id, supabaseClient: supabase };
}

/**
 * Creates a Supabase client with the service-role key (bypasses RLS).
 * Use only for internal operations that must not be blocked by RLS,
 * such as writing payment_attempts rows.
 */
export function createServiceClient() {
  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceKey) {
    throw new Error("SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY not configured");
  }
  return createClient(supabaseUrl, serviceKey, { auth: { persistSession: false } });
}