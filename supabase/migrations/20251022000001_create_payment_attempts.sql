-- Migration: Create payment_attempts table for PayPal Sandbox integration
-- This table tracks payment attempts for ₱400 PHP consultation appointments
-- Run this migration in Supabase Dashboard → SQL Editor after review

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create payment_attempts table
CREATE TABLE IF NOT EXISTS payment_attempts (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    pet_id          uuid NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    paypal_order_id text UNIQUE,                    -- NULL until PayPal order created
    paypal_request_id text,                          -- Server-controlled PayPal-Request-Id for retries
    idempotency_key text NOT NULL,                   -- Client-generated, scoped to owner
    amount_cents    int  NOT NULL DEFAULT 40000,     -- ₱400.00 in cents
    currency        char(3) NOT NULL DEFAULT 'PHP' CHECK (currency = 'PHP'),
    status          text NOT NULL DEFAULT 'PENDING_ORDER'
                    CHECK (status IN ('PENDING_ORDER','CREATED','APPROVED','CAPTURED','FAILED','EXPIRED')),
    capture_id      text UNIQUE,                     -- PayPal capture ID, set on CAPTURED
    approval_url    text,                            -- PayPal approval URL for redirect
    paypal_response jsonb,                           -- Minimal capture object for audit
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    expires_at      timestamptz NOT NULL DEFAULT (now() + interval '3 hours')
);

-- Composite unique index for client-scoped idempotency (enforces one attempt per owner per idempotency key)
CREATE UNIQUE INDEX IF NOT EXISTS idx_payment_attempts_owner_idempotency
    ON payment_attempts (owner_id, idempotency_key);

-- Additional indexes for common queries
CREATE INDEX IF NOT EXISTS idx_payment_attempts_owner_id
    ON payment_attempts (owner_id);

CREATE INDEX IF NOT EXISTS idx_payment_attempts_pet_id
    ON payment_attempts (pet_id);

CREATE INDEX IF NOT EXISTS idx_payment_attempts_status
    ON payment_attempts (status);

-- Enable Row Level Security
ALTER TABLE payment_attempts ENABLE ROW LEVEL SECURITY;

-- RLS Policies
-- Policy: Owners can read their own payment attempts
CREATE POLICY "payment_attempts_owner_select" ON payment_attempts
    FOR SELECT USING (auth.uid() = owner_id);

-- Policy: Service role (Edge Functions) can insert payment attempts
-- This bypasses RLS via the service-role key
CREATE POLICY "payment_attempts_service_insert" ON payment_attempts
    FOR INSERT WITH CHECK (true);

-- Policy: Service role can update payment attempts (for capture, status changes)
CREATE POLICY "payment_attempts_service_update" ON payment_attempts
    FOR UPDATE USING (true) WITH CHECK (true);

-- Note: No DELETE policy - payment attempts should never be deleted for audit trail

-- Updated_at trigger (optional, if you have the common updated_at trigger function)
-- CREATE TRIGGER update_payment_attempts_updated_at
--     BEFORE UPDATE ON payment_attempts
--     FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Comment on table
COMMENT ON TABLE payment_attempts IS 'Tracks PayPal payment attempts for ₱400 consultation appointments. One row per unique (owner_id, idempotency_key).';